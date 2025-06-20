## Changelog
* feat: added chest pressure system
* feat: added power multiplier
* feat: added speedLimiter property (if false, you can accelerate beyond max speed)
* feat: added sanding mechanism (WIP, not balanced yet)
* feat: added handbrake to all rolling stocks (remove independent brake except from locomotives)
* feat/ref: better pressure brake (WIP)
* feat: added changeable brake mode (instant = old instant brake pressure; default = old braking pressure; realistic = with emergency braking if brake is 100%)
* feat: added dynamic brake to diesel locomotives
* feat: added slipping speed to speed readout (you can now see, if your locomotive is slipping)
* feat: added better turntable model (WIP)
* ref: static tractive effort calculation based on json entry ("tractive_effort_lbf")
* ref: applied tractive effort calculation based on chest pressure calculation
* ref: chuff sound and cylinder drain trigger
* ref: updated steam engine ui overlay
* ref: change cylinder drains > 0.9 instead of = 1.0
* ref: no scrolling on press or toggle widgets anymore (it was horrible)

## new config values
* sand efficiency
* power Multiplier (currently only for steam locomotives)
* brake mode (instant, default, realistic)

## new json entries
"properties": {
    "hand_brake": true (all)
    "handbrake_coefficient": 1.0 (all)
    "speed_limiter": true (locomotive)
    "dynamic_brake_factor": 1.0 (diesel, is the dynamic braking force in Newtons, 0 is disabled)
    "piston_diameter": 1.0 (steam, in meters)
    "piston_stroke": 1.0 (steam, in meters)
    "wheel_diameter": 1.0 (steam, in meters)
    "cylinder_count": 2 (steam)
    "power_multiplier": 1.0 (steam, we recommend 1.3 for mixed locomotives, 1.2 for high speed locomotives and 1.4-1.5 for cargo locomotives)
}

## new model component parts
* DYNAMIC_BRAKE_X (control)
* SANDING_CONTROL_X
* HAND_BRAKE_X (control)
* GAUGE_HAND_BRAKE_X
* GAUGE_DYNAMIC_BRAKE_X

## new readouts
* CHEST_PRESSURE
* HAND_BRAKE
* BRAKE_CYLINDER_PRESSURE
* DYNAMIC_BRAKE

## new stats
* CHEST_PRESSURE
* MAX_CHEST_PRESSURE
* BRAKE_CYLINDER_PRESSURE

## new hotkeys
* sanding control
* increase handbrake
* zero handbrake
* decrease handbrake
* increase dynamic brake
* zero dynamic brake
* decrease dynamic brake

## turntable track (WIP)
* RAIL_LEFT
* RAIL_RIGHT
* RAIL_TURNTABLE (for rotating platform)
* RAIL_BASE (for turntable ground)