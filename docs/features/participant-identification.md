# Participant Identification

## Home
The homescreen shows an overview of the different steps that an operator goes through in a participant workflow.
This is the screen that will be returned to after every participant registration or visit logging.

![Homescreen](../images/screenshots/home.png ':size=500')

## Participant ID

The participant ID can be directly typed into the text field, or scanned from a barcode.
This step can be skipped.

The barcode scanner is based on the `zxing` [library](https://github.com/zxing/zxing) and supports the following formats:

| 1D product            | 1D industrial | 2D             |
|:----------------------|:--------------|:---------------|
| UPC-A                 | Code 39       | QR Code        |
| UPC-E                 | Code 93       | Data Matrix    |
| EAN-8                 | Code 128      | Aztec          |
| EAN-13                | Codabar       | PDF 417        |
| UPC/EAN Extension 2/5 | ITF           | MaxiCode       |
|                       |               | RSS-14         |
|                       |               | RSS-Expanded   |

![Participant ID](../images/screenshots/participant-id.png ':size=500')

## Iris Scan

The iris scan makes use of the [Neurotechnology VeriEye SDK](https://www.neurotechnology.com/verieye.html).
Each iris scan step can be skipped, so iris matching can happen with either one or both eyes or skipped altogether.

A glasses visual is used to give the operator a visual indication of which eye should be scanned. When the USB iris scanner is used, the image is overlayed over the correct lens of the glasses.

To perform iris scanning, a Neurotechnology IrisExtractor license and a USB iris scanner are required, see [Getting started](getting-started?id=configuring-the-iris-scan-license).

In the 'debug' variant of the apk, an option is included to load the iris scan from an image included with the application.  This facilitates testing the application in an emulator.
For the 'release' variant, this is option removed.

![Iris Scan](../images/screenshots/iris-scan.png ':size=500')

## Participant Matching

In order to do participant matching, at least one of the identification methods needs to have been completed.
If all identification steps were skipped, an error dialog will be shown, and the operator will be taken back to the home screen.

Participant matches will be loaded from the backend and are ordered by decreasing iris matching score. Matches for both the participant ID and the iris scan are returned.
The matched participants are divided into two categories:
1. **Participants registered at this operator site:** these are shown at the top of the list, and can be selected.
2. **Participants registered at other sites:** only the participant ID and the site are shown. These participants cannot be selected for logging visits.

![Participant Matching](../images/screenshots/participant-matching.png ':size=500')

For each participant at this site, the following are shown:
* Participant photo, if available
* Participant ID
* Gender
* Year of birth
* Address
* Vaccine
* One or more icons indicating whether this participant was matched based on participant ID or iris scan.

Upon tapping a participant at the current site, the background is highlighted and a button with the label 'Match Selected Participant' will appear in the lower-right of the screen. Tapping this button will proceed to the [Visit screen](features/visit-logging) for this selected participant.

Alternatively, the operator can tap the 'New Participant' button in the lower-left corner if no matches were found, or if the found matches are not corresponding with the participant present. This will open the [Participant Registration flow](features/participant-registration).