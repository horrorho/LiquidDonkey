![LOGO](https://raw.githubusercontent.com/wiki/horrorho/LiquidDonkey/images/logo_small.png?raw=true)
## LiquidDonkey 
Download iCloud backups. 

### * iOS 9 breaking changes *
Backups created with iOS 9 devices are unrecoverable with LiquidDonkey. Until the underlying mechanism for the new API are described, this situation will not change. I apologise in advance.

### What is it?
Java command-line tool to download iCloud backups, reworked from [iLoot](https://github.com/hackappcom/iloot), [iphone-dataprotection](https://code.google.com/p/iphone-dataprotection/) and [mobileme](https://code.google.com/p/mobileme/) scripts. All copyrights belong to their respective owners.

**This tool is for educational purposes only. Make sure it's not illegal in your country before use.**
### Why LiquidDonkey?
- Speed. Python crytographical functions may bottleneck backups with slow staggered downloads.
- Multi-threading. More consistent bandwidth utilization.
- Tolerance. Options for persistent retrieval algorithms, tested over a lamentable Wi-Fi connection.
- Resume backups. Interrupted downloads to the same folder continue where they left off, unless specified otherwise.
- Filtering. More options to download the files you want to.
- Additional connection options. Including relaxed SSL verification.
- Free! MIT license.

### Build
Requires [Java 8 JRE/ JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Maven](https://maven.apache.org).

[Download](https://github.com/horrorho/LiquidDonkey/archive/master.zip), extract and navigate to the LiquidDonkey folder:

```
~/LiquidDonkey $ mvn package
```
The executable Jar is located at /target/LiquidDonkey.jar

### Usage
```
~/LiquidDonkey/target $ java -jar LiquidDonkey.jar --help
usage: LiquidDonkey [OPTION]... (<token> | <appleid> <password>)
 -o,--output <arg>             Output folder.
 -c,--combined                 Do not separate each snapshot into its own
                               folder.
 -u,--udid <hex>               Download the backup/s with the specified
                               UDID/s. Will match partial UDIDs. Leave
                               empty to download all.
 -s,--snapshot <int>           Only download data in the snapshot/s
                               specified.
                               Negative numbers indicate relative
                               positions from newest backup with -1 being
                               the newest, -2 second newest, etc.
    --item-types <item_type>   Only download the specified item type/s:
                               ADDRESS_BOOK(addressbook.sqlitedb)
                               CALENDAR(calendar.sqlitedb)
                               CALL_HISTORY(call_history.db) PHOTOS(.jpg
                               .jpeg) MOVIES(.mov .mp4 .avi) PNG(.png)
                               SMS(sms.db) VOICEMAILS(voicemail)
                               NOTES(notes)
 -d,--domain <str>             Limit files to those within the specified
                               application domain/s.
 -r,--relative-path <str>      Limit files to those with the specified
                               relative path/s
 -e,--extension <str>          Limit files to those with the specified
                               extension/s.
    --min-date <date>          Minimum last-modified timestamp, ISO format
                               date. E.g. 2000-12-31.
    --max-date <date>          Maximum last-modified timestamp, ISO format
                               date. E.g. 2000-12-31.
    --min-size <Kb>            Minimum size in kilobytes.
    --max-size <Kb>            Maximum size in kilobytes.
 -f,--force                    Download files regardless of whether a
                               local version exists.
 -p,--persistent               More persistent in the handling of network
                               errors, for unstable connections.
 -a,--aggressive               Aggressive retrieval tactics.
 -t,--threads <int>            The maximum number of concurrent threads.
    --relax-ssl                Relaxed SSL verification, for SSL
                               validation errors.
 -x,--stack-trace              Prints stack trace on errors, useful for
                               debugging.
    --token                    Output authentication token and exit.
    --help                     Display this help and exit.
    --version                  Output version information and exit.

```
Download all files:

```
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password
Authenticating.
Listed backups:

1:	Device:	iPad Air XXXXX (iPad4,2)
	SN:	XXXXXXXXXXXX
	UDID:	XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	Size:	67.1 MB
	Snapshots:
	    1 >	James's iPad 8.0.2   41.2 MB  Wed, 15 Mar 2015 13:25:32 GMT
	   14 >	James's iPad 8.0.2   12.2 MB  Tue, 3 Nov 2014 03:45:12 GMT
	   15 >	James's iPad 8.0.2   13.7 MB  Wed, 21 Mar 2015 01:15:20 GMT

2:	Device:	iPhone 6 XXXXX (iPhone7,2)
	SN:	XXXXXXXXXXXX
	UDID:	XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	Size:	4.4 GB
	Snapshots:
	    1 >	James's iPhone 8.4    4.1 GB  Tue, 4 Aug 2015 12:41:38 +0100
	  125 >	James's iPhone 8.4  122.2 MB  Mon, 3 Aug 2015 13:28:01 +0100
	  126 >	James's iPhone 8.4  155.4 MB  Tue, 4 Aug 2015 12:11:34 +0100

Select backup/s to download (leave blank to select all, q to quit):
: 1
Selected backup/s: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Retrieving backup: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Retrieving snapshot: 15 (James's iPad)
	HomeDomain Library/SpringBoard/LockBackgroundThumbnail.jpg Success.

```
Download photos only to the specified output folder:

```
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --item-types photos --output ~/backups/iCloud
```
Download photos over an unstable internet connection, e.g. bad Wi-Fi reception:

```
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --item-types photos --persistent
```
By default if you're saving to the same directory, DonkeyLooter will carry on where it left off. It will not download those files again unless forced to:

```
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --output ~/backups/iCloud --force
```
By default DonkeyLooter will separate out snapshots into their respective sub-folders, unless combining is specified:

```
~/LiquidDonkey/target $ java -jar LiquidDonkey/target/LiquidDonkey.jar me@icloud.com password --output ~/backups/iCloud --combined
```
### Addendum
Although it works wonderfully, consider this project to be in the alpha state. I will introduce, amend and test features as time permits. Please report any bugs. Developed on an Ubuntu Linux box. Functional on VM Windows 7 and VM OS X Yosemite 10.10.

### Help!
I would be grateful if any knowledgeable persons would be able to assist with:
- The MBSFile block specifies a 160-bit file checksum/ signature.  It's not plain SHA-1. What is the algorithm?

