import {NgModule} from '@angular/core';
import {AuthorityColorPipe} from "./common/ui/authority-color.pipe";
import {IconComponent} from "./common/ui/icon/icon.component";
import {InfiniteScrollDirective} from "./common/ui/infinite-scroll.directive";

@NgModule({
  declarations: [
    AuthorityColorPipe,
    IconComponent,
    InfiniteScrollDirective,
  ],
  exports: [
    AuthorityColorPipe,
    IconComponent,
    InfiniteScrollDirective,
  ]
})
export class AppExportsModule { }
