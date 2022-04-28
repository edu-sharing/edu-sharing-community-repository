# This is the angular core-ui-module for edu-sharing.

The core ui module provides basic ui modules (e.g. list objects, toast messages) for reuse in other projects

Make sure to include the core-module as well as this one depends on it. Please also checkout the information from the core-module.

This module also depends on some dependencies, including:

```
@angular/material
ngx-toasty
@ngx-translate/core
```

Make sure to install the dependencies. Take a look at the edu-sharing repository package.json to see the current used dependency versions.

As some components rely on global styles, make sure to include the "styles/core-base.scss" into your angular-project by adding it to the angular.json style section.
