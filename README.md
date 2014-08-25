NFCulT
======

NFC Ultralight Toolkit

NFCulT is an application for exploitation of MF Ultralight implementations created by Matteo Beccaro from the security firm SecureNetwork s.r.l.
https://www.securenetwork.it


It use the NFC of your android phone to exploit the already known vulnerabilities such as:

* Lock Attack
* Time Attack
* Replay Attack

and it can also help research to develop new exploit.
This first release is still in beta, a more complete and stable version will be released in September.

The app has been released at BlackHat US. Arsenal.
https://www.blackhat.com/us-14/arsenal.html#Beccaro

The vulnerabilities the app exploits are listed here:

Lock Attack: 
https://www.defcon.org/images/defcon-21/dc-21-presentations/bughardy-Eagle1753/DEFCON-21-bughardy-Eagle1753-OPT-circumventing-in-MIFARE-ULTRALIGHT-WP-Updated.pdf

Time Attack:
http://media.ccc.de/browse/congress/2013/30C3_-_5479_-_en_-_saal_6_-_201312291215_-_building_a_safe_nfc_ticketing_system_-_bughardy_-_eagle1753.html

=MODES=

#LOCK ATTACK:
Lock Attack mode exploit the OTP vulnerabilities. It will overwrite your Lock sector, locking the OTP page which become then read-only.

#TIME ATTACK:
Time Attack mode is a bit more complex. First of all you have set your own paramenters using "Settings" bottom. Here you have to set your 0 time, from when the stamping machine starts counting for the timestamp, and in which pages it have to be written.
Then simple set your own timestamp and write on your ticket.

#REPLAY ATTACK:
For replay attack you need to read a valid ticket first, write the saved dump on your UID changable mifare ultralight clone, and then stamp it. Once all these operations have been done, write back the stamped clone ticket on your real one, or use the clone. Once the clone has been used, you can rewrite on it the previous dump, and start again.
In Manage Dump section you can rename and delete saved dumps.

#CUSTOM EDIT:
The Custom Edit mode allow you to read every single page of your ticket and edit each bit and write it back. It is very usefull for finding the 0 time for the Time Attack or for develop new attacks as well.
The Fix UID bottom is usefull when you broke the UID, it will set it back as a Mifare Ultralight. ( Note: You can break and fix UID only with UID Changable clone ticket ).

=FUTURE=

I plan to release a new version of the application by the end of September, it will probably have the following features:

. XML Preferences: The user can create an XML files in which he can set all preferences to use in the app ( 0time, OTP mode, timestamp mode, etc )
. Better graphics
. Cleaner code

=HELP&SUGGESTIONS=

For any info, helps or suggestions you can contact me at m.beccaro@securenetwork.it
