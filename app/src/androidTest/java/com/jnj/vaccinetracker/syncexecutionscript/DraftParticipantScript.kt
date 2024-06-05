package com.jnj.vaccinetracker.syncexecutionscript

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.di.DaggerTestDaggerComponent
import com.jnj.vaccinetracker.readResource
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.soywiz.klock.DateTime
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@LargeTest
class DraftParticipantScript {

    private val app = appContext.applicationContext as VaccineTrackerApplication

    @Inject
    lateinit var participantManager: ParticipantManager

    @Inject
    lateinit var configurationManager: ConfigurationManager

    @Inject
    lateinit var visitManager: VisitManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var syncSettingsRepository: SyncSettingsRepository

    @Inject
    lateinit var participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository

    private val androidFiles = AndroidFiles(app)


    // Participant Settings
    private val prefix = "td434d1-"
    private val startNumber = 1
    private val endNumber = 200

    @Before
    fun setUp() {
        val comp = DaggerTestDaggerComponent.builder().application(app).build()
        comp.inject(this)
    }


    /**
     * before you run make sure you're logged in with the operator
     */
    @Suppress("RedundantNullableReturnType")
    @Test
    fun bulkParticipantRegistrationScript(): Unit = runBlocking {
        // Participant Settings
        val siteUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException()
        val vaccine = findVaccine("Covid 3D vaccine")
        val address = Address(
            address1 = "Koekoekstraat",
            address2 = "40",
            cityVillage = "Beerse",
            stateProvince = null,
            country = "Belgium",
            countyDistrict = null,
            postalCode = "2340"
        )

        val pictureResourceName: String? = "test_script_image.jpeg"
        val image = pictureResourceName?.let { readPicture(it) }
        val telephone: String? = null
        val birthDate = DateTime(1999, 6, 6)
        val isBirthDateEstimated = false
        val lang = "en"
        val gender = Gender.MALE
        val template: BiometricsTemplateBytes? = readAssetsTemplate()
        // Registration
        (startNumber..endNumber).forEach { participantNumber ->
            val participantId = formatParticipantId(participantNumber)
            println("creating participant with id: $participantId")
            val draftParticipant = participantManager.registerParticipant(
                participantId = participantId,
                nin = "NIN$participantId",
                gender = gender,
                birthDate = birthDate,
                isBirthDateEstimated = isBirthDateEstimated,
                telephone = telephone,
                siteUuid = siteUuid,
                language = lang,
                vaccine = vaccine,
                address = address,
                picture = image,
                biometricsTemplateBytes = template
            )
            println("logging first visit for participant $participantId")
            val participantUuid = draftParticipant.participantUuid
            val visits = visitManager.getVisitsForParticipant(participantUuid = participantUuid)
            val firstScheduledDosingVisit = visits.find {
                it.visitType == Constants.VISIT_TYPE_DOSING
                        && it.visitStatus == Constants.VISIT_STATUS_SCHEDULED && it.dosingNumber == 1
            } ?: error("can't find first scheduled dosing visit for participant $participantId")
            val vialCode = "vial-$participantId"
            val manufacturerName = "AstraZenica"
            val manufacturer = findManufacturer(draftParticipant, manufacturer = manufacturerName)
            visitManager.registerDosingVisit(
                participantUuid = draftParticipant.participantUuid,
                encounterDatetime = dateNow(),
                visitUuid = firstScheduledDosingVisit.uuid,
                manufacturer = manufacturer,
                vialCode = vialCode,
                dosingNumber = firstScheduledDosingVisit.dosingNumber!!
            )
        }
    }

    /**
     * before you run make sure you're logged in with the operator
     */
    @Suppress("RedundantNullableReturnType")
    @Test
    fun bulkParticipantMatchingScript(): Unit = runBlocking {
        // Config
        // NOTE please enable matching license for tablet or put this to **false**
        val checkTemplate = false


        val resultsMap = mutableMapOf<String, MatchingSuccess>()
        val template: BiometricsTemplateBytes = readAssetsTemplate()
        (startNumber..endNumber).forEach { participantNumber ->
            val participantId = formatParticipantId(participantNumber)
            println("looking up $participantId")
            val results = participantManager.matchParticipants(participantId = participantId, null, biometricsTemplateBytes = if (checkTemplate) template else null)
            val result = results.firstOrNull()
            val success = result != null
            val hasPicture = if (result != null) {
                val picture = try {
                    participantManager.getPersonImage(result.uuid)
                } catch (ex: Exception) {
                    logError("error during fetching picture for $participantId", ex)
                    null
                }
                picture != null
            } else false
            val hasTemplate = result?.isBiometricsMatch(template) ?: false
            val hasTemplateDownloaded = if (checkTemplate) {
                val downloadedTemplate = participantBiometricsTemplateRepository.findByParticipantId(participantId)
                downloadedTemplate?.toFile(androidFiles)?.exists() == true
            } else {
                false
            }
            resultsMap[participantId] =
                MatchingSuccess(hasPicture = hasPicture,
                    isFound = success,
                    isTemplateMatched = hasTemplate,
                    hasTemplateDownloaded = hasTemplateDownloaded,
                    checkTemplate = checkTemplate)
        }
        val idsNotFounds = resultsMap.filter { !it.value.isSuccess() }.map { it.key }
        if (idsNotFounds.isNotEmpty()) {
            val errorParts = mutableListOf<String>()
            idsNotFounds.forEach { pId ->
                val result = resultsMap[pId]!!
                val part = buildString {
                    append("$pId found: ${result.isFound}, picture: ${result.hasPicture}")
                    if (checkTemplate) {
                        append(", template matched: ${result.isTemplateMatched}, template downloaded: ${result.hasTemplateDownloaded}")
                    }
                }
                errorParts += part
            }
            val idsString = errorParts.joinToString("\n")
            throw Exception("Following participant ids not found:\n $idsString")
        } else {
            println(buildString {
                append("successfully found all participant ids including pictures")
                if (checkTemplate)
                    append(" and templates")
            })
        }
    }

    private fun readPicture(fileName: String): ImageBytes = runBlocking(Dispatchers.IO) {
        readResource(fileName).buffered().use { it.readBytes() }.let { ImageBytes(it) }
    }

    /**
     * @return template with two eyes from following assets:
     *
     *          - iris_image_b1.jpg
     *          - iris_image_b2.jpg
     */
    private fun readAssetsTemplate(): BiometricsTemplateBytes = runBlocking(Dispatchers.IO) {
        readResource("test_script_iris_template.dat").buffered().use { it.readBytes() }.let { BiometricsTemplateBytes(it) }
    }

    private fun formatParticipantId(number: Int) = "$prefix$number"

    private suspend fun findVaccine(vaccineName: String): String {
        val vaccines = configurationManager.getConfiguration().vaccines
        return vaccines.find { it.name.equals(vaccineName, ignoreCase = true) }?.name ?: error("vaccine $vaccineName doesn't exist")
    }

    private suspend fun findManufacturer(draftParticipant: DraftParticipant, manufacturer: String): String {
        val regimen = requireNotNull(draftParticipant.regimen) { "draft participant ${draftParticipant.participantId} doesn't have a regimen" }
        val manufacturers = configurationManager.getVaccineManufacturers(regimen)
        return manufacturers.find { it.equals(manufacturer, ignoreCase = true) } ?: error("Couldn't find $manufacturer in $manufacturers")
    }

    class MatchingSuccess(val hasPicture: Boolean, val isTemplateMatched: Boolean, val hasTemplateDownloaded: Boolean, val isFound: Boolean, val checkTemplate: Boolean) {
        fun isSuccess() = hasPicture && (!checkTemplate || (isTemplateMatched && hasTemplateDownloaded)) && isFound
    }
}