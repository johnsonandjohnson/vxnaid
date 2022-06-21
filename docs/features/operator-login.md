# Operator Login

The login page requests the operator credentials. These credentials will be used for all API calls to the backend.

![Login](../images/screenshots/login.png ':size=500')

## Login

The login call will receive a session token. As long as this session is valid, the operator will not be required to log in again, and will go straight to the participant workflow screen when opening the app.
This session token is injected in all the API calls to the backend.

The session validity duration can be configured in the backend system. In our demo system, this is set to 24 hours.

## Account Creation
The Android application does not have functionality for registering a new operator. Please see the backend documentation for information on how to create operator accounts and assign the correct permissions.

## Settings
On the top-right of the login screen, tapping the 'cog'-icon will reveal the settings dialog.

The Backend URL field configures the backend server (or load balancer) to connect to. This value needs only be set once, and is then saved to the device.
If the reset icon to the right of this field is tapped, it will be reset to the default value configured in the source code.

![Settings](../images/screenshots/backend_config.png ':size=500')
