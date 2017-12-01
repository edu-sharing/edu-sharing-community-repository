import { CollectionsMainComponent } from './collections.component';
import { CollectionNewComponent } from './collection-new/collection-new.component';

export var ROUTES_COLLECTIONS =[
  {path: 'collections', component: CollectionsMainComponent },
  {path: 'collections/collection/:mode/:id', component: CollectionNewComponent}
];
