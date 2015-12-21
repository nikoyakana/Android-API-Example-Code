###Changelog 
#### Version 3.1
* Renamed `NymiDevice` to `NymiProvision`
* Renamed `NymiSignatureKey` to `NymiPublicKey`

#####General Changes
* Removed ~~`"Nymi"`~~ prefix from all callbacks

#####Nymi Adapter:
* New methods:
    * `clearCallbacks()` 
    * `isDiscoveryEnabled()` 
    * `isInitialized()` 
    * `setAgreementCallback()`
    * `setDeviceApproachedCallback()`
    * `setDeviceDetectedCallback()`
    * `setDeviceFoundCallback()`
    * `setDeviceFoundStatusChangeCallback()`
    * `setDevicePresenceChangeCallback()`
    * `setDiscoveryEnabled()`
    * `setFirmwareVersionCallback()`
    * `setNewNymiNonceCallback()`
    * `setNewProvisionCallback()`
    * `setPattern()`
    * `setProximityEstimateChangeCallback()`
    * `verifyPartnerAndSign()`
* Changed methods
    * Changed signature of `getSymmetricKey()`, `requestInfo()`, `setPattern()`, `startProvision()` from `boolean` to `void`
    * `getInstance()` renamed to `getDefaultAdapter()`
    * `NymiProvisionCallback` split into `AgreementCallback` and `NewProvisionCallback`
    * Removed pattern argument from `AgreementCallback.onAgreement()`
* Deleted methods:
    * ~~`finish()`~~
    * ~~`startProvision()`~~
* New interfaces
    * `DeviceApproachedCallback`
    * `DeviceDetectedCallback`
    * `DeviceFoundCallback`
    * `DeviceFoundStatusChangeCallback`
    * `DevicePresenceChangeCallback`
    * `DiscoveryModeChangeCallback`
    * `FirmwareVersionCallback`
    * `NewAdvNonceCallback`
    * `NewProvisionCallback`
    * `PartnerVerifiedCallback`
    * `ProximityEstimateChangeCallback`

#####NymiProvision
* New methods:
    * `addPartner()`
* New interfaces:
    * `PartnerAddedCallback`
