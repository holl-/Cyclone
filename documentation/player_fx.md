# Cyclone JavaFX UI

Package `player.fx`


## Controls and Skins

PlayerControl &rarr; RoundPlayerSkin

CircularSlider &rarr; CircularSliderSkin


## Usage

**PlayerWindow** &rarr; PlayerControl

**RoundPlayerSkin** &rarr; CircularSlider, Buttons


## Events

Registered with PlayerControl.

Skins copy and re-fired for encapsulating controls via
`getNode().fireEvent(e.copyFor(...))`.

CircularSliderSkin &rarr; 