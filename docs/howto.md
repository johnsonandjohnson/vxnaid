# Vaccine tracker Android application
Tablet application for use by operators in the field to identify or register vaccination campaign participants,
check eligibility for dosages and log visits.


## Mock Backend Performance test

1) Switch build variant (bottom right) to manualMockBackend
2) Run app
3) Go through setup wizard
4) in login screen, click on overflow and the mock settings
5) Enter desired target participants count (i.e 250K)
6) Click on the generate participants button
7) You'll see sync in progress banner, timer will be activated and participant count will be displayed.
Flight mode is allowed, it will not disrupt the sync. Leave the device on day and night, check up once a day.

## Update Neurotechnology JNA lib
when installing a new MegaMatcher version, the jna.jar and libjnidispatch.so needs to be downloaded from github
as they're not provided in the SDK folders.
1) in the 'Neurotechnology Biometric SDK.pdf' look up the jna version they expect
2) navigate to https://github.com/java-native-access/jna/tree/<<versiono>>/dist
3) Download the jna.jar file and replace the existing one in libs folder
4) Download android-x86.jar, unpack it and put the libjnidispatch.so in the libs/x86 folder
5) Download android-armv7.jar, unpack it and put the libjnidispatch.so in the libs/armeabi-v7a folder