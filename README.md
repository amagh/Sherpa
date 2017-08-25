# Hiker-Guide

(WIP) Android Application for hikers to use to discover new trails and guide them when they go. Experienced hikers can document trails
they have taken; showing the trail conditions, difficult spots, and the reward that awaits them when other hikers follow their guides. 
New hikers can use the app to discover trails and gain the confidence that they can complete a trail with the guide in their hands.

By default, the app shows the most recenty added Guides to the online repository, but allows the option to search for guides by location
(e.g. Yosemite Valley or Glacier National Park). Users who would like to contribute can create an account and begin posting their own 
guides and adding reviews of other guides letting others know of the quality of the guide. 

When viewing Guides, users are presented a detailed overview of the trail including a map they can use to view their relative position
on the map. If would like, users can cache a copy of the guide and map to local storage to be used on the trail when they have no 
network connections.

Users of the app who decide not to create an account can still favorite items and save guides to local storage.

# Additional Information

This application features the use of:

  - MVVM design
  - Firebase Realtime Database
  - Firebase Storage
  - Geofire for searching indexed guides by proximity to search location
  - Schematic for building ContentProvider
  - GPX Parser for parsing GPX files to Polylines
  - Calculation of distances using Java Geodesy Library and Vincenty's Formula
  - Android Additive Animations library for animations
  - Android FilePicker for selecting local files
