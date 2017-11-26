Reading me is dangerous!
This mod is highly experimental.

# Minecraft hight mod


## what is this
This allows you to build, interact and see the minecraft world up to 65536 on
 the y coordinate.

## Save format
 All sections that are above what vanilla would load are saved separately in a
 new file with the same name as original region + an "e" at the end.
 This makes it possible to load vanilla worlds and hopefully not destroying
 them (don't believe me, __it's at your own risk!__). All entities and tile entities
 are stored in the original region. Offsets in the region file with an "e"
 (region extension file) are longs instead of ints to fit larger chunks, because
 of that timestamps are in the 3rd sector. Size of a chunk is basically
 unlimited.

## worldgen
The only difference should be that structures (trees etc.) are not getting cut
on 255y, but the heightmap is same as in vanilla, so you will not find mountains
above 255. Version with extended heightmap is being experimented with and may be
published later.

## lag
This requires a ton of ram, multiple gigabytes may be allocated on the lowest
render distance. Skylight updates get extremely long time to finish, so if you
 want to build something very high, consider trying it in the End or Nether
 instead. Generating a world takes longer as more sections are being checked for
 skylight etc. I did not experience major frame rate lag.
