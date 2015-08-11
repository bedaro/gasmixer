An Android gas mixing application. Computes the required pressures of oxygen and helium required to blend nitrox and trimix for SCUBA diving applications.
  * It's able to handle starting with a non-empty cylinder.
  * The gas used to top up the mix is air by default, but this can be changed if you are interested in using another banked gas.
  * Top-up mode to determine what you would get if you topped up an existing mix to a desired pressure.
  * It also computes MODs, EADs, and ENDs for desired and result mixes.
  * Supports both ideal and Van der Waals equations for mixing calculations
  * Can switch between adding Oxygen or Helium first when blending
  * Supports imperial or metric units. Scuba Tanks and Gas Mixer synchronize the units setting with each other so you don't have to make the same change in both applications.
  * Can customize the temperature setting
  * Should run well on most Android devices running Cupcake (1.5) or higher.
  * Support for nitrox and trimix continuous blending
  * Source code is available, providing developers with a good implementation of common SCUBA formulae and performing Van der Waals mixing calculations numerically.

Gas Mixer is available for download on the [Android Market](https://market.android.com/details?id=divestoclimb.gasmixer) and on SlideME (an experiment for countries without support for paid apps on the Android Market). SCUBA Tanks, the companion app that allows specifying tank sizes for mixing, is available for download here and is also on the Android Market and SlideME.

Always analyze your gas! This program is only making approximations. It is no substitute for proper blending technique and an analyzer.

Recent changes:

  * Gas Mixer 3.0: Redesigned blend result window that simplifies the presentation of blend steps, and shows volumes of gas added
  * Scuba Tanks 2.0.1: Fix a crash from preparing notifications
  * Gas Mixer 2.8.7: Built with new backend classes written for Scuba Tanks 2.0 which should fix a force close or two. Top-up starting gas helium percentage can be set in 1% increments, top-up gas itself can have less than 5% O2 to allow a pure helium top-up.
  * Scuba Tanks 2.0: New features like storing hydro/VIP dates, serial numbers. Notifications when cylinders about to expire. Automatic backups and restores to SD card.

If you'd like to learn more about how gas blending works in Gas Mixer using Van der Waals equations, read MixingMath.