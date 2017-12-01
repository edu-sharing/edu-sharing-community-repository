- Each component (a individual edu sharing component like workspace, recycle, collections...) has to be placed under "modules"
	It needs the following files
		declarations.ts -> Contains all Declarations of components, the exported var name should be DECLARATIONS_MODULENAME
		providers.ts -> Contains all Declarations of services (if it has any), the exported var name should be PROVIDERS_MODULENAME
		routes.ts -> Contains all routes for this module, the exported var name should be ROUTES_MODULENAME
The following files have to be changed after a component is added:
	app.module.ts -> Import the DECLARATIONS_MODULENAME and PROVIDERS_MODULENAME
	router/router.component.ts -> Add the ROUTES_MODULENAME to the routes list
