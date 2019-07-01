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

## Translation:

You have to choose whether you want to provide the i18n files yourself (place them in assets/i18n/<language>.json) or if you want to use the once provided by the repository (recommended).

Either way, in your main class, you have to initalize the translations and prepare them for loading.
```
constructor(
    private translate : TranslateService,
    private config : ConfigurationService,
    private session : SessionStorageService,
    private route : ActivatedRoute
  ){
    // use the repository language files as source
    Translation.setSource(TranslationSource.Repository);
    Translation.initialize(this.translate,this.config,this.session,this.route).subscribe(()=>{

    });
}
```
