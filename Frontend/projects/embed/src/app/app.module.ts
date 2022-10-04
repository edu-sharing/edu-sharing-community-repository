import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AppModule as EduSharingModule } from 'src/app/app.module';

// We need to just import `EduSharingModule` to avoid compile errors due to imports to Angular
// declarables that are not part of this app.
void EduSharingModule;

@NgModule({
    declarations: [AppComponent],
    imports: [BrowserModule, AppRoutingModule],
    providers: [],
    bootstrap: [AppComponent],
})
export class AppModule {}
