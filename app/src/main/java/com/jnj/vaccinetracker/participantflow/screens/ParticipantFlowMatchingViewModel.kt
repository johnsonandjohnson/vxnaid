package com.jnj.vaccinetracker.participantflow.screens

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.mappers.toDomain
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.ParticipantMatch
import com.jnj.vaccinetracker.common.domain.entities.Site
import com.jnj.vaccinetracker.common.domain.usecases.GetAddressMasterDataOrderUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetTempBiometricsTemplatesBytesUseCase
import com.jnj.vaccinetracker.common.exceptions.LicensesNotObtainedException
import com.jnj.vaccinetracker.common.exceptions.MatchNotFoundException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.model.DisplayValue
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.soywiz.klock.DateFormat
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject

class ParticipantFlowMatchingViewModel @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository,
    private val participantManager: ParticipantManager,
    private val configurationManager: ConfigurationManager,
    private val resourcesWrapper: ResourcesWrapper,
    override val dispatchers: AppCoroutineDispatchers,
    private val getAddressMasterDataOrderUseCase: GetAddressMasterDataOrderUseCase,
    private val getTempBiometricsTemplatesBytesUseCase: GetTempBiometricsTemplatesBytesUseCase,
) : ViewModelBase() {

    private val retryClickEvents = eventFlow<Unit>()
    val noIdentifierUsed = eventFlow<Unit>()
    private val args = stateFlow<Args?>(null)
    val loading = mutableLiveBoolean()
    val items = mutableLiveData<List<MatchingListItem>>()
    val selectedParticipant = mutableLiveData<ParticipantUiModel>()
    private val selectedParticipantImage = mutableLiveData<ParticipantImageUiModel?>()
    val matchCount = mutableLiveInt()
    val errorMessage = mutableLiveData<String>()
    val progress = mutableLiveInt()
    val launchRegistrationFlowEvents = eventFlow<Unit>()
    val participantIdGenerated = mutableLiveData<String>()

    init {
        initState()
    }

    data class Args(val participantId: String?, val phone: String?, val irisScans: Map<IrisPosition, Boolean>?) {
        val isNoIdentifierUsed get() = listOfNotNull(participantId, phone, irisScans).isEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun load(
        args: Args,
    ) {
        errorMessage.set(null)
        matchCount.set(null)
        loading.set(true)
        try {
            val sites = configurationManager.getSites()
            val irisScans = args.irisScans
            val phoneQuery = args.phone
            val participantIdQuery = args.participantId
            val biometricsTemplateBytes = irisScans?.let { getTempBiometricsTemplatesBytesUseCase.getBiometricsTemplate(it) }
            val matches = participantManager.matchParticipants(
                participantId = participantIdQuery,
                phone = phoneQuery,
                biometricsTemplateBytes = biometricsTemplateBytes,
                onProgressPercentChanged = { progress.value = it }
            )
            val locationUuid = syncSettingsRepository.getSiteUuid()
            if (locationUuid == null) {
                logError("Failed to match participants: We don't have a location UUID")
                errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            } else {
                handleSuccessfulMatchResponse(
                    responseParticipants = matches,
                    sites = sites,
                    locationUuid = locationUuid,
                    participantIdQuery = participantIdQuery,
                    participantPhoneQuery = phoneQuery,
                    biometricsQuery = biometricsTemplateBytes
                )
            }
            loading.set(false)
        } catch (ex: LicensesNotObtainedException) {
            if (ex.isObtainableAfterForceClose)
                errorMessage.set(resourcesWrapper.getString(R.string.msg_no_iris_license_must_force_close))
            else
                errorMessage.set(resourcesWrapper.getString(R.string.participant_matching_msg_no_iris_license))
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            if (isNoMatchResponse(ex)) {
                items.set(listOf(SubtitleItem(text = resourcesWrapper.getString(R.string.participant_matching_label_no_results_this_site))))
            } else {
                logError("Failed to match participants: ", ex)
                errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            }
            loading.set(false)
        }
    }

    fun onNewParticipantButtonClick() {
        launchRegistrationFlowEvents.tryEmit(Unit)
    }

    private fun initState() {
        errorMessage.set(null)
        matchCount.set(null)
        loading.set(true)
        args.filterNotNull().distinctUntilChanged().combine(retryClickEvents.asFlow())
        { args, _ ->
            load(args)
        }.launchIn(scope)
        retryClickEvents.tryEmit(Unit)
    }

    fun setArguments(args: Args) {
        if (args.isNoIdentifierUsed) {
            logWarn("noIdentifierUsed")
            noIdentifierUsed.tryEmit(Unit)
            loading.set(false)
            return
        }
        logInfo("emitting args: {}", args)
        this.args.tryEmit(args)
    }

    fun onRetryClick() {
        retryClickEvents.tryEmit(Unit)
    }

    private fun isNoMatchResponse(throwable: Throwable): Boolean {
        return throwable is MatchNotFoundException || throwable.cause is MatchNotFoundException
    }

    private suspend fun handleSuccessfulMatchResponse(
        responseParticipants: List<ParticipantMatch>,
        sites: List<Site>,
        locationUuid: String,
        participantIdQuery: String?,
        participantPhoneQuery: String?,
        biometricsQuery: BiometricsTemplateBytes?,
    ) {
        loading.set(false)
        val itemsList = ArrayList<MatchingListItem>()

        val mappedParticipants = mapResponseParticipantToAppParticipant(responseParticipants, locationUuid, participantIdQuery, participantPhoneQuery, biometricsQuery, sites)
            .filter { it.siteName != null }

        matchCount.set(mappedParticipants.size)

        // Split into the participants on this site and on other sites
        val (siteParticipants, nonSiteParticipants) = mappedParticipants.partition { it.isCurrentSite }
        logInfo("siteParticipants, nonSiteParticipants: ${siteParticipants.size}, ${nonSiteParticipants.size}")
        // If no results for this site, show message
        if (siteParticipants.isEmpty()) {
            itemsList.add(SubtitleItem(text = resourcesWrapper.getString(R.string.participant_matching_label_no_results_this_site)))
        }
        itemsList.addAll(siteParticipants)

        // If results for other sites, show message
        if (nonSiteParticipants.isNotEmpty()) {
            // Create a subtitle for the others
            itemsList.add(SubtitleItem(text = resourcesWrapper.getString(R.string.participant_matching_label_other_results)))
            itemsList.addAll(nonSiteParticipants)
        }

        items.set(itemsList)

        // Load photo only for the participants from this site
        siteParticipants.forEach {
            loadParticipantPicture(it.participant.participantUUID)
        }
    }

    private suspend fun mapResponseParticipantToAppParticipant(
        responseParticipants: List<ParticipantMatch>,
        locationUuid: String,
        participantIdQuery: String?,
        participantPhoneQuery: String?,
        templateQuery: BiometricsTemplateBytes?,
        sites: List<Site>,
    ): List<AnyParticipantItem> = withContext(dispatchers.computation) {
        val loc = configurationManager.getLocalization()
        fun List<String>.translate(): String = joinToString(", ") { loc[it] }
        fun findSite(siteUuid: String?) = sites.find { site -> site.uuid == siteUuid }?.let { DisplayValue(it.name, loc[it.name]) }
        responseParticipants.map { participant ->
            val participantLocationUuid = participant.locationUuid
            val site = findSite(participantLocationUuid)
            if (site == null) {
                logError("site == null for location $participantLocationUuid")
            }
            if (locationUuid == participantLocationUuid) {
                val addressMasterDataOrder =
                    getAddressMasterDataOrderUseCase.getAddressMasterDataOrder(participant.address?.country, isUseDefaultAsAlternative = true, onlyDropDowns = false)
                ParticipantItem(
                    participant = ParticipantUiModel(
                        participantUUID = participant.uuid,
                        participantId = participant.participantId,
                        irisMatchingScore = participant.matchingScore,
                        birthDateText = participant.birthDate.toDateTime().format(DateFormat.FORMAT_DATE),
                        isBirthDateEstimated=participant.isBirthDateEstimated,
                        gender = participant.gender,
                        telephone = participant.telephoneNumber,
                        homeLocation = participant.address?.toDomain()?.toStringList(addressMasterDataOrder)?.translate(),
                        vaccine = participant.vaccine?.let { DisplayValue(it, loc[it]) },
                        siteUUID = participantLocationUuid
                    ),
                    picture = null,
                    isCurrentSite = true,
                    participantIdMatch = participant.isParticipantIdMatch(participantIdQuery),
                    participantPhoneMatch = participant.isPhoneMatch(participantPhoneQuery),
                    isIrisMatch = participant.isBiometricsMatch(templateQuery),
                    siteName = site
                )
            } else {
                OtherSiteParticipantItem(
                    participant = ParticipantUiModel(
                        participantId = participant.participantId,
                        matchingScore = participant.matchingScore,
                        siteUUID = participantLocationUuid
                    ),
                    isCurrentSite = false,
                    siteName = site
                )
            }
        }
    }

    private fun loadParticipantPicture(participantUUID: String?) {
        if (participantUUID == null) return
        scope.launch {
            val participantImageBytes = try {
                participantManager.getPersonImage(participantUUID)
            } catch (ex: Throwable) {
                yield()
                ex.rethrowIfFatal()
                logDebug("No picture for participant $participantUUID")
                return@launch
            }
            updateParticipantPicture(participantUUID, participantImageBytes.toUiModel())
        }
    }

    private fun updateParticipantPicture(participantUUID: String, participantImageBytes: ParticipantImageUiModel) {
        items.set(items.get()?.map {
            if (it is ParticipantItem && it.participant.participantUUID == participantUUID) it.copy(picture = participantImageBytes) else it
        })
    }

    fun setSelectedParticipant(matchingListItem: MatchingListItem) {
        if (matchingListItem !is ParticipantItem) return
        selectedParticipant.set(matchingListItem.participant)
        selectedParticipantImage.set(matchingListItem.picture)
    }

    fun getSelectedParticipantSummary(): ParticipantSummaryUiModel? {
        return selectedParticipant.get()?.let {
            ParticipantSummaryUiModel(
                participantUuid = it.participantUUID ?: return null,
                participantId = it.participantId ?: return null,
                gender = it.gender ?: return null,
                birthDateText = it.birthDateText ?: return null,
                isBirthDateEstimated = it.isBirthDateEstimated ?: return null,
                vaccine = it.vaccine ?: return null,
                participantPicture = selectedParticipantImage.get()
            )
        }
    }

    sealed class MatchingListItem {
        abstract val type: ItemType
    }

    sealed class AnyParticipantItem : MatchingListItem() {
        abstract val participant: ParticipantUiModel
        abstract val isCurrentSite: Boolean
        abstract val siteName: DisplayValue?
    }

    data class ParticipantItem(
        override val type: ItemType = ItemType.PARTICIPANT,
        override val participant: ParticipantUiModel,
        val picture: ParticipantImageUiModel?,
        override val isCurrentSite: Boolean,
        val participantIdMatch: Boolean,
        val participantPhoneMatch: Boolean,
        val isIrisMatch: Boolean,
        override val siteName: DisplayValue?,
    ) : AnyParticipantItem()

    data class OtherSiteParticipantItem(
        override val type: ItemType = ItemType.OTHERSITEPARTICIPANT,
        override val participant: ParticipantUiModel,
        override val isCurrentSite: Boolean,
        override val siteName: DisplayValue?,
    ) : AnyParticipantItem()

    data class SubtitleItem(
        override val type: ItemType = ItemType.SUBTITLE,
        val text: String,
    ) : MatchingListItem()

    enum class ItemType(val value: Int) {
        PARTICIPANT(0),
        OTHERSITEPARTICIPANT(1),
        SUBTITLE(2)
    }

}
