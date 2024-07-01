package com.jnj.vaccinetracker.register.dialogs

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.AddressTree
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.GetAddressMasterDataOrderUseCase
import com.jnj.vaccinetracker.common.exceptions.AddressNotFoundException
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.model.DisplayValue
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.register.data.mapper.AddressMapper
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class HomeLocationPickerViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val resourcesWrapper: ResourcesWrapper,
    override val dispatchers: AppCoroutineDispatchers,
    private val addressMapper: AddressMapper,
    private val getAddressMasterDataOrderUseCase: GetAddressMasterDataOrderUseCase,
) : ViewModelBase() {

    companion object {
        const val ITEM_NOT_IN_LIST = "Not in list!"

    }

    private val selectedCountryInternal = stateFlow<String?>(null)
    val loading = mutableLiveBoolean()
    val countries = mutableLiveData<List<DisplayValue>>()
    val addressInputFields = mutableLiveData<List<AddressInputField>>()
    val country = mutableLiveData<String>()
    val apiErrorMessage = mutableLiveData<String>()

    private val rootNode = AddressTree("root")

    private val userInput = mutableMapOf<AddressValueType, UserInput?>()
    private var addressFields: List<AddressField>? = null

    init {
        initState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun load() = coroutineScope {
        val config = try {
            val site = syncSettingsRepository.getSiteUuid()?.let { configurationManager.getSiteByUuid(it) } ?: throw NoSiteUuidAvailableException()
            val config = configurationManager.getConfiguration()
            val loc = configurationManager.getLocalization()
            countries.set(config.addressFields.keys.sortedBy { it }.map { DisplayValue(it, loc[it]) })
            setCountry(site.country)
            config
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("Failed to get country list: ", ex)
            apiErrorMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_country_api))
            null
        }
        if (config != null) {
            selectedCountryInternal.filterNotNull()
                .onEach {
                    country.value = it
                }.mapLatest { country ->
                    loadCountryData(config, country)
                    loading.set(false)
                }.launchIn(this)
        } else {
            loading.set(false)
        }
    }

    private fun buildAddressFields(config: Configuration, country: String): List<AddressField> {
        return config.addressFields[country] ?: throw AddressNotFoundException(country)
    }

    /**
     * Attempts to load the country address fields and hierarchy master data for the given country from the configuration.
     * If fails with a non-fatal exception, error message will be set.
     *
     * @param configuration Configuration object from which to load the address fields
     * @param country       Country for which to retrieve the address hierarchy
     */
    private suspend fun loadCountryData(configuration: Configuration, country: String) {
        try {
            val addressFields = buildAddressFields(configuration, country)
            loading.set(true)
            val addressHierarchy = configurationManager.getCountryAddressHierarchy(country)

            val substancesConfig = configurationManager.getSubstancesConfig()


            onSelectedCountryDataLoaded(country, addressFields, addressHierarchy)
            apiErrorMessage.set(null)
            loading.set(false)
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            logError("Failed to get country list: ", throwable)
            apiErrorMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_country_api))
            loading.set(false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initState() {
        scope.launch {
            loading.set(true)
            load()
        }
    }

    fun setCountry(country: String) {
        this.selectedCountryInternal.value = country
    }

    private suspend fun onSelectedCountryDataLoaded(country: String, addressFields: List<AddressField>, addressHierarchy: List<List<AddressValue>>) {
        initUserInputs(country, addressFields)
        this.addressFields = addressFields

        withContext(dispatchers.io) {
            val countryNode = rootNode.getOrCreateChild(country) // Add the current country to the root address tree

            // Build the address tree from the address hierarchy
            addressHierarchy.forEach { addressParts ->
                var parentNode = countryNode
                for (addressField in addressFields.filter { it.inputType == AddressField.InputType.DROPDOWN }) {
                    val value = addressParts.find { it.type == addressField.field }?.value
                    if (value != null) {
                        parentNode = parentNode.getOrCreateChild(value)
                    } else {
                        logWarn("addressField ${addressField.field} not found in address parts, breaking loop early")
                        break
                    }
                }
            }

            updateInputFields(addressFields)

            if (addressFields.any { it.inputType == AddressField.InputType.DROPDOWN }) {
                require(addressHierarchy.isNotEmpty()) { "Address Hierarchy master data must not be empty where dropdown address fields are defined" }
            }
        }
    }

    private fun getLastUserInputIndexOfNotNull(): Int {
        return userInput.entries.indexOfLast { (_, value) -> value?.isValid() ?: false }
    }

    private fun initUserInputs(country: String, addressFields: List<AddressField>) {
        val countryItem = UserInput.Dropdown.Item(country)
        if (userInput[AddressValueType.COUNTRY] != countryItem) {
            //this means country was changed
            userInput.clear()
        }
        userInput[AddressValueType.COUNTRY] = countryItem //always put country first. [addressFields] doesn't contain country
        addressFields.forEach { addressField ->
            if (!userInput.containsKey(addressField.field)) {
                userInput[addressField.field] = null
            }
        }
    }

    private suspend fun updateInputFields(addressFields: List<AddressField>) {
        val loc = configurationManager.getLocalization()
        val lastFilledInFieldIndex = getLastUserInputIndexOfNotNull() // 0 is country value, 1 is first AddressField value
        val lastDropdownIndex = addressFields.indexOfLast { it.inputType == AddressField.InputType.DROPDOWN }
        val hasUserInputNotInThisList = userInput.values.any { it?.isNotInList() ?: false }
        val amountOfItemsToShow =
            if (lastFilledInFieldIndex >= lastDropdownIndex + 1 || hasUserInputNotInThisList) { // + 1 to compensate for country (= input at index 0)
                addressFields.size // Show all fields because the rest of the fields are free input
            } else {
                var additionalFreeInputsToShow = 0
                if (lastFilledInFieldIndex >= 0) {
                    while (addressFields.drop(lastFilledInFieldIndex + additionalFreeInputsToShow)
                            .firstOrNull()?.inputType == AddressField.InputType.FREE_INPUT
                    ) {
                        //increment while next item after last filled in index is free input
                        additionalFreeInputsToShow++
                    }
                }
                lastFilledInFieldIndex + additionalFreeInputsToShow + 1  // Show all fields that have been selected, plus one extra that the user should fill in next
            }
        val inputFields = createInputFields(addressFields, amountOfItemsToShow, loc)

        addressInputFields.set(inputFields)
    }

    private suspend fun createInputFields(
        addressFields: List<AddressField>,
        amountOfItemsToShow: Int,
        loc: TranslationMap
    ): List<AddressInputField> {
        // Build the list of input fields from the addressFields and which fields the user has filled in
        var inputFields = addressFields
            .take(amountOfItemsToShow)
            .map { addressField ->
                AddressInputField(
                    displayName = loc[addressField.name],
                    type = when (addressField.inputType) {
                        AddressField.InputType.DROPDOWN -> AddressInputFieldType.DROPDOWN
                        AddressField.InputType.FREE_INPUT -> AddressInputFieldType.FREE_INPUT
                    },
                    dropdownValues = null,
                    userInput = userInput[addressField.field],
                    errorMessage = null,
                    addressField = addressField,
                    previousDropdowns = null
                )
            }

        val dropdownInputFields = inputFields
            .filter { it.addressField.inputType == AddressField.InputType.DROPDOWN }
        // loop through input fields and set previousDropdowns
        inputFields = inputFields.map { inputField ->
            val dropDownOnlyIndex = dropdownInputFields.indexOfFirst { it === inputField }

            val parents = if (inputField.addressField.inputType == AddressField.InputType.DROPDOWN && dropDownOnlyIndex > 0) {
                0.until(dropDownOnlyIndex).map { i -> dropdownInputFields[i] }
            } else {
                null
            }
            inputField.copy(previousDropdowns = parents)
        }
        //loop through input fields and adjust type and set dropdown values
        inputFields = inputFields.map { inputField ->
            val type = when {
                inputField.userInput?.isNotInList() ?: false -> AddressInputFieldType.NOT_IN_LIST
                inputField.previousDropdowns?.any { it.userInput?.isNotInList() ?: false } ?: false -> AddressInputFieldType.FREE_INPUT
                else -> inputField.type
            }
            inputField.copy(type = type, dropdownValues = getDropdownValues(inputField, loc))
        }
        return inputFields
    }

    private suspend fun getDropdownValues(inputField: AddressInputField, loc: TranslationMap): List<DisplayValue>? {
        if (inputField.addressField.inputType != AddressField.InputType.DROPDOWN) {
            logInfo("getDropdownValues: not a dropdown so returning null")
            return null
        }
        val country = country.value
        if (country.isNullOrBlank()) {
            logInfo("getDropdownValues: country not selected so returning null")
            return null
        }
        return withContext(dispatchers.io) {
            // start at root node which contains the countries
            var currentNode: AddressTree? = rootNode
            // move to selected country
            currentNode = currentNode?.getNode(country)
            // traverse over previous selected values
            val parentValues = inputField.previousDropdowns?.map { it.userInput as? UserInput.Dropdown.Item }?.map { it?.value }.orEmpty()
            val parentValuesNonNull = parentValues.filterNotNull()
            if (parentValuesNonNull.size == parentValues.size) {
                parentValuesNonNull.forEach { input ->
                    currentNode = currentNode?.getNode(input)
                }
                currentNode?.run {
                    children.map { it.data }
                        .let { data ->
                            listOf("") + data + ITEM_NOT_IN_LIST
                        }.let { data ->
                            data.map { DisplayValue(it, loc[it]) }
                        }
                }
            } else {
                val previousDropdownEntries = inputField.previousDropdowns?.map { it.addressField.field to it.userInput }
                logInfo("getDropdownValues: parent values {} has some null fields [field:${inputField.addressField.field}, type:${inputField.type}]", previousDropdownEntries)
                null
            }

        }
    }

    private fun AddressInputField.toUserInput(selectedItem: String, isDropdownValue: Boolean): UserInput {
        return when (type) {
            AddressInputFieldType.FREE_INPUT -> UserInput.FreeInput(selectedItem)
            AddressInputFieldType.DROPDOWN -> when (selectedItem) {
                ITEM_NOT_IN_LIST -> UserInput.Dropdown.NotInThisList(selectedItem)
                else -> UserInput.Dropdown.Item(selectedItem)
            }
            AddressInputFieldType.NOT_IN_LIST -> when {
                selectedItem == ITEM_NOT_IN_LIST || !isDropdownValue -> UserInput.Dropdown.NotInThisList(selectedItem)
                else -> UserInput.Dropdown.Item(selectedItem)
            }
        }
    }

    fun onAddressFieldSelected(addressInputField: AddressInputField, selectedItem: String, isDropdownValue: Boolean) = scope.launch {
        logInfo("onAddressFieldSelected: ${addressInputField.addressField.field} ${addressInputField.type} $selectedItem $isDropdownValue")
        val userInputValue = addressInputField.toUserInput(selectedItem, isDropdownValue = isDropdownValue)
        if (userInputValue == userInput[addressInputField.addressField.field]) return@launch

        if (isDropdownValue) {
            // If you select a dropdown value, clear the selected dropdown items below this dropdown
            userInput.keys.dropWhile { it != addressInputField.addressField.field }.forEach { key ->
                userInput[key] = null
            }
        }
        userInput[addressInputField.addressField.field] = userInputValue
        if (isDropdownValue) {
            addressFields?.let { updateInputFields(it) }
        }
    }

    private fun Map<AddressValueType, String>.toAddress(): Address {
        return addressMapper.toDomain(this)
    }

    fun confirmAddress(confirmationListener: (AddressUiModel) -> Unit) = scope.launch {
        if (!isInputValid()) {
            logWarn("Address validation failed!")
            return@launch
        }
        val loc = configurationManager.getLocalization()
        val country = requireNotNull(country.get()) { "country must not be null" }
        val resultAddressMap = mutableMapOf(
            AddressValueType.COUNTRY to country
        )
        userInput.forEach { (addressFieldType, value) ->
            logInfo("confirmAddress $addressFieldType $value")
            value?.let { userInput ->
                resultAddressMap[addressFieldType] = userInput.value
            }
        }
        val masterDataFields = getAddressMasterDataOrderUseCase.getAddressMasterDataOrder(country, isUseDefaultAsAlternative = false, onlyDropDowns = false)
        val address = resultAddressMap.toAddress()
        val stringRepresentation = address.toStringList(masterDataFields).joinToString(" | ") { loc[it] }

        confirmationListener(AddressUiModel(address, stringRepresentation))
    }

    private fun isInputValid(): Boolean {
        if (loading.value)
            return false

        if (country.value.isNullOrBlank())
            return false

        var result = true
        addressInputFields.set(addressInputFields.get()?.map { inputField ->
            val input = userInput[inputField.addressField.field]
            if (input?.isValid() != true) {
                result = false
                logInfo("displayName = ${inputField.displayName} ${inputField.type}")
                inputField.copy(
                    userInput = input, // Make sure to update the userInput so the UI reflects what the user filled in previously
                    errorMessage = if (inputField.type == AddressInputFieldType.DROPDOWN) {
                        resourcesWrapper.getString(R.string.home_location_picker_error_select_x, inputField.displayName)
                    } else {
                        resourcesWrapper.getString(R.string.home_location_picker_error_enter_x, inputField.displayName)
                    }
                )
            } else {
                inputField.copy(
                    userInput = input, // Make sure to update the userInput so the UI reflects what the user filled in previously
                    errorMessage = null
                )
            }
        })

        userInput.forEach { (_, value) ->
            if (value?.isValid() != true) result = false
        }

        return result
    }

    data class AddressUiModel(val addressMap: Address, val stringRepresentation: String)

    data class AddressInputField(
        val displayName: String,
        val type: AddressInputFieldType,
        val dropdownValues: List<DisplayValue>?,
        val userInput: UserInput?,
        val errorMessage: String?,
        val addressField: AddressField,
        val previousDropdowns: List<AddressInputField>?,
    ) {
        val parent: AddressInputField? get() = previousDropdowns?.lastOrNull()
    }

    enum class AddressInputFieldType {
        FREE_INPUT,
        DROPDOWN,
        NOT_IN_LIST
    }

    private fun UserInput.isValid() = value.isNotEmpty() && when (this) {
        is UserInput.Dropdown.Item -> true
        is UserInput.Dropdown.NotInThisList -> value != ITEM_NOT_IN_LIST
        is UserInput.FreeInput -> true
    }

    sealed class UserInput {
        abstract val value: String

        fun isNotInList() = this is Dropdown.NotInThisList

        sealed class Dropdown : UserInput() {
            data class Item(override val value: String) : Dropdown()
            data class NotInThisList(override val value: String) : Dropdown()
        }

        data class FreeInput(override val value: String) : UserInput()
    }

}

