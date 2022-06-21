# Visit Logging

With a participant matched or a new one registered, the operator can log a visit.
At the top of the screen, the participant's main details are repeated for reference:
* Participant photo, if available
* Participant ID
* Gender
* Year of birth
* Vaccine

The operator has two options for a visit:
1. Dosing Visit
2. Other Visit

## Dosing Visit

The dosing visit corresponds with the administration of a vaccine dose. As such, it is only possible when an open dosing visit is available for the participant.
If multiple dosing visits are still open for the participant, the first (oldest) open visit is shown.

If an open dosing visit is available, the following items are shown on the screen:
* Visit window: shows the date range within which the dosage is supposed to be administered. If the current date is within this range, it is displayed in green, otherwise in red. The dosing visit can still be registered outside the window, but a dialog box requesting confirmation will be shown.
* Dosing number
* Vial identification number entry: The vaccine vial number can be entered manually or by scanning a barcode. The barcode scanning utilizes the same implementation as for the participant ID entry. This entry is mandatory.

## Other Visit

The other visit can be used for any other visit type that should be logged, such as an in-person follow-up visit. For this type of visit, no additional information is required and can be submitted at any time.

![Other Visit](../images/screenshots/other-visit.png ':size=500')