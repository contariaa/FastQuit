## FastQuit

### About this mod

This mod lets you return to the title screen early while your world is still saving in the background.

Don't worry, the mod will wait for the world to finish saving when necessary, no data will be lost!

The world will show up on the world list, and you can even edit, delete or recreate it while it is still saving.
This is done by reusing the info from the still saving world.

In its current state this should be safe to do as measures for thread safety have been taken.
If you do somehow still encounter an issue, the worst thing to happen will be the action failing and Minecraft showing you a toast informing you of it.

**If that happens, please [open an issue](#problems) with your .minecraft/logs/latest.log file attached!**

### Join the Discord!

If you are interested in my mods or have any questions, you can join my [Discord](https://discord.gg/B6ZV8SF672)!

### Configure

To configure the mod, open the config screen from [ModMenu](https://modrinth.com/mod/modmenu) or go to .minecraft/config/fastquit-config.txt.

There is currently three options to configure:

**Show Toasts**:

Determines whether a toast gets shown when a world finishes saving.

**Background Thread Priority**:

Sets the thread priority of the server when saving worlds in the background.
This is done to improve client performance while saving, but will make the saving take longer over all.
Value has to be between 0 and 10, setting it to 0 will disable changing thread priority.

**Render "Saving world" Screen**:

When playing on high render distance, quitting the world can still take a bit because the client-side chunk storage has to be cleared.
By enabling this setting the "Saving world" screen will be rendered.

**Show Saving Time**

Determines whether the time it took to save the world gets displayed on toasts and the world list.
Value has to be between 0 and 2, with 0 never showing the time, 1 only on the toast and 2 also on the world list. In ModMenu these are accordingly labelled.

### How does this mod work?

**At it's core, this mod is quite simple:**

Normally, when quitting a singleplayer world, the client will wait until the server thread has finished saving the world before going to the Title Screen.
This mod skips this waiting period and lets the server continue saving in the background.

**PLEASE NOTE** this does not always make quitting a world instant, as the client can still take a bit of time to unload rendered chunks etc. However, even in that case you will see a speedup as clientside unloading and serverside saving now perform simultaneously instead of one after the other.

**Of course in practice this is not quite that easy:**

When working with multiple threads like this, many problems arise.
For example, when going to the world list, Minecraft will try to read its level data but fails because the background saving still locks the world file.

Instead, we now access the information through the still saving server.
Similarly, when trying to edit, delete or recreate the world, we have to use the existing session.

To keep these processes thread safe, a lot of synchronization has to be done to ensure the saving server and the client don't get in each other's way.

For some things, like rejoining the still saving world or making a backup, we just wait for the server to finish saving completely.
We also wait for the worlds to finish saving when quitting the game to ensure no data is lost.

### Thanks to...

#### Translators:
- Felix14-v2 (Russian)
- Cccc-owo (Simplified Chinese)
- JustAlittleWolf (German)
- YakisikliBaran (Turkish)
- Skidamek (Polish)
- Oondanomala (Italian)
- Madis0 (Estonian)
- notlin4 (Traditional Chinese)
- BurrConnie (Ukrainian)
- GodGun968 (Simplified Chinese)
- ImVietnam (Vietnamese)
- ishi-sama (French)
- KAGUYA-1137 (Japanese)
- demorogabrtz (Portuguese)
- ReallySuperOtter (Swedish)
- asabhi6776 (Hindi)
####

### Compatibility

With the help of [MixinExtras](https://github.com/LlamaLad7/MixinExtras) and a custom Mixin Config Plugin, the mixins have been designed to be very non-intrusive while also being very effective.

Fast Backups:
- (FIXED in FastQuit v3.1.0+) crash on world exit when "Allow multiple running Worlds" is ON
- (FIXED in Fast Backups v0.17+) crash when quitting Minecraft immediately after world exit, fixed in newer versions of Fast Backup

Wakes:
- (FIXED in Wakes v0.4.0+) crash on world exit with Wakes 0.3.0

If you suspect an incompatibility with another mod, please [open an issue](#problems)!

### Problems?

If you are experiencing any problems, please open an issue on the [issue tracker](https://github.com/KingContaria/FastQuit/issues)!

Make sure to attach important info like other mods you were using, log- or crash-files and if possible steps to reproduce.

Before uploading any files make sure to get rid of personal data! (For example your computers username in logged directories)