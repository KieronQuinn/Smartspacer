# Target: Weather Forecast

### Feature ID

`SmartspaceTarget.FEATURE_WEATHER_ALERT` (`10`)

### Preview

![image](https://user-images.githubusercontent.com/3430869/142783015-18a1faae-de13-4e74-aacf-76e6e7f7c91a.png)

### Requirements

Base Action must be set, with below extras

| Extra | Type | Notes |
| - | - | - |
| temperatureValues | String Array | Max items 4, size will limit following arrays |
| weatherIcons | Array of Bitmaps [Serializable] | Max items 4, does not seem to work (casting crash) |
| timestamps | String Array | Max items 4 |

