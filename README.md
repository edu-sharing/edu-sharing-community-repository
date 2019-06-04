This is the angular core-module for edu-sharing.

The core module provides all essential services, objects and interfaces to access the repository api.

Also, multiple helper and common formatting features are included to provide continous user experience across ui's that use this module.

This module is a subtree in the edu-sharing main repository, found at Frontend/src/app/core-module


This module also has dependencies to the RouterModule and HttpClientModule from Angular:

Include this module by importing it:

```
  imports: [
    BrowserModule,
    CoreModule,
    CoreBridgeModule,
    RouterModule.forRoot([]),
    HttpClientModule
  ],
```

  
Some callbacks of this module make use of the "BridgeModule". Your application has to implement this module and the service.

Take a look at the BridgeModule of the edu-sharing repository.