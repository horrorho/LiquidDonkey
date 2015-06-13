![LOGO](https://raw.githubusercontent.com/wiki/horrorho/LiquidDonkey/images/logo_small.png?raw=true)
## LiquidDonkey 
Download iCloud backups. 

### What is it?
Java command-line tool to download iCloud backups, reworked from [iLoot](https://github.com/hackappcom/iloot), [iphone-dataprotection](https://code.google.com/p/iphone-dataprotection/) and [mobileme](https://code.google.com/p/mobileme/) scripts. All copyrights belong to their respective owners.

**This tool is for educational purposes only. Make sure it's not illegal in your country before use.**
### Why LiquidDonkey?
- Speed. Python crytographical functions may bottleneck backups with slow staggered downloads.
- Multi-threading. More consistent bandwidth utilization. Optimized for smaller file groups across multiple threads.
- Tolerance. Options for persistent retrieval algorithms, tested over a lamentable Wi-Fi connection.
- Resume backups. Interrupted downloads to the same folder continue where they left off, unless specified otherwise.
- Filtering. More options to download the files you want to.
- Additional connection options. Including relaxed SSL verification.
- Free! MIT license.

### Build
Requires [Java 8](https://www.java.com) and [Maven](https://maven.apache.org).

[Download](https://github.com/horrorho/LiquidDonkey/archive/master.zip), extract and navigate to the LiquidDonkey folder:

```bash
~/LiquidDonkey $ mvn package
```
The executable Jar is located at /target/LiquidDonkey.jar

### Usage
```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey.jar --help
usage: DonkeyLooter appleid password [OPTION]...
 -o,--output <arg>          Output folder.
 -c,--combined              Do not separate each snapshot into its own
                            folder.
 -u,--udid <arg>            Download the backup/s with the specified
                            UDID/s. Will match partial UDIDs. Leave empty
                            to download all.
 -s,--snapshot <arg>        Only download data in the snapshot/s
                            specified.
                            Negative numbers indicate relative positions
                            from newest backup with -1 being the newest,
                            -2 second newest, etc.
    --item-types <arg>      Only download the specified item type/s:
                            ADDRESS_BOOK (addressbook.sqlitedb) CALENDAR
                            (calendar.sqlitedb) CALL_HISTORY
                            (call_history.db) PHOTOS (.jpg .jpeg) MOVIES
                            (.mov .mp4 .avi) PNG (.png) SMS (sms.db)
                            VOICEMAILS (voicemail) NOTES (notes)
 -d,--domain <arg>          Limit files to those within the specified
                            application domain/s.
 -r,--relative-path <arg>   Limit files to those with the specified
                            relative path/s
 -e,--extension <arg>       Limit files to those with the specified
                            extension/s.
    --min-date <arg>        Minimum last-modified timestamp, ISO format
                            date. E.g. 2000-12-31.
    --max-date <arg>        Maximum last-modified timestamp ISO format
                            date. E.g. 2000-12-31.
    --min-size <arg>        Minimum size in kilobytes.
    --max-size <arg>        Maximum size in kilobytes.
 -f,--force                 Download files regardless of whether a local
                            version exists.
 -p,--persistent            More persistent in the handling of network
                            errors, for unstable connections.
 -a,--aggressive            Aggressive retrieval tactics.
 -t,--threads <arg>         The maximum number of concurrent threads.
    --relax-ssl             Relaxed SSL verification, for SSL validation
                            errors.
 -x,--stack-trace           Prints stack trace on errors, useful for
                            debugging.
    --help                  Display this help and exit.
    --version               Output version information and exit.
```
Download all files:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password
Authenticating: me@icloud.com
Listed backups:

0:	Name:	My iPad
	Device:	iPad Air XXXXX
	SN:	XXXXXXXXXXXX
	UDID:	XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	iOS:	8.0.2
	Size:	38.9 MB (Snapshot/s: 1 15 16)
	Last:	Wed, 21 Mar 2015 07:25:30 GMT

1:	Name:	My iPhone
	Device:	iPhone 6 XXXXX
	SN:	XXXXXXXXXXXX
	UDID:	XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	iOS:	8.3
	Size:	7.5 GB (Snapshot/s: 1 100 101)
	Last:	Fri, 12 Jun 2015 12:45:26 +0100

Select backup/s to download (leave blank to select all, q to quit):
: 0

Selected backups: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Downloading backup: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Retrieving snapshot: 1
Fetching 1780/1780.
	CameraRollDomain Media/DCIM/102APPLE/IMG_2063.JPG
	CameraRollDomain Media/DCIM/102APPLE/IMG_2078.JPG
	CameraRollDomain Media/DCIM/101APPLE/IMG_1887.JPG
```
Download all files:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password
```
Download photos only to the specified output folder:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --item-types photos --output ~/backups/iCloud
```
Download photos over an unstable internet connection, e.g. bad Wi-Fi reception:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --item-types photos --persistent
```
By default if you're saving to the same directory, DonkeyLooter will carry on where it left off. It will not download those files again unless forced to:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --output ~/backups/iCloud --force
```
By default DonkeyLooter will separate out snapshots into their respective sub-folders, unless combining is specified:

```bash
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --output ~/backups/iCloud --combined
```
### Addendum
Although it works wonderfully, consider this project to be in the alpha state. I will introduce, amend and test features as time permits. Please report any bugs. I currently only have access to a Linux box with VM Windows 7, therefore I'm unable to test on Mac OS X.

### Help!
I would be grateful if any knowledgeable persons would be able to assist with:
- The iCloud API call for retrieving all the available snapshots, rather than just the latest.
- The MBSFile block specifies a 160-bit file checksum/ signature.  It's not plain SHA-1. What is the algorithm?

