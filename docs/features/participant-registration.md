# Participant Registration

## Participant Photo
The first step in the registration flow is taking a photo of the participant. This step is optional and can be skipped.

To facilitate the correct positioning of the participant, an oval outline is overlayed onto the camera view. The operator should aim to frame the participant's face within this oval.

Once a photo is taken, the operator can choose to submit this photo or retake it if the quality is not as desired.

When registering, this participant photograph is compressed and resized to limit bandwidth usage.

Submitting a photo or skipping this step, brings the operator to the Participant Details screen.

## Participant Details

At the top of the screen, the information gathered for the participant so far is summarized:
* Participant ID: This ID can still be corrected manually or rescanned from a barcode
* Iris scan: indication of which eyes were scanned
* Participant photo: if taken, this will be shown on the top-right

The following additional details can be captured for the participant:
* Gender: Male, Female or Other
* Year of birth: To be entered in YYYY format, where 1900 is the earliest possible entry.
* Mobile phone: Phone number (including country code) for the participant to receive reminder or follow-up messages.
* Home location: Participant address
* Vaccine: The vaccine that should be administered to the participant. This selection determines also the visit/dosing regimen that the participant should adhere to.
* Language: Language in which the participant will receive any reminder or follow-up messages to the supplied mobile phone number.

![Participant Details](../images/screenshots/participant-details.png ':size=500')

All fields are mandatory, with exception of the mobile phone number. If one of the mandatory fields is not completed, or a field entry does not pass validation, the corresponding fields are highlighted to indicate this error.

Once a participant is successfully registered, the operator is given the choice whether to continue with [logging a visit](features/visit-logging) for this participant, or to restart the [participant identification](features/participant-identification) workflow.

### Mobile Phone

The mobile found is entered as a combination of the country code and the phone number.

The country code is implemented using the [Country Code Picker Library](https://github.com/hbb20/CountryCodePickerProject), where the operator can search for the correct country code through a dialog box. The default country code is set according to the country of the operator site selected in the [Site Selection](features/site-selection) screen.

The phone number is automatically formatted according to the country code selecting, through usage of the [libphonenumber Library](https://github.com/google/libphonenumber). This is also used to check if the entered phone number is valid according to phone number specification for that country.

Phone number entry is optional. When no mobile phone is entered upon submitting the registration, the operator is presented with a dialog box requesting confirmation. Registering a participant without a mobile phone means that no reminder or follow-up messages can be sent.

### Home Location

The participant address is entered in a separate dialog box, by tapping the 'Set Home Address' button. The operator is presented with a series of dropdown boxes that represent a hierarchical definition of the address. These dropdown boxes are populated with master data from the backend system. Should a participant's home address not be included in the master data, the operator can select the 'Not in list!' entry. All further fields in the hierarchy must then be manually entered.

The first field in the hierarchy is the country field. This field again defaults to the country of the operator site selected in the [Site Selection](features/site-selection) screen.