import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ComuneComponent } from './comune/comune.component';
import { HomeComponent } from './home/home.component';
import { MapComponent } from './map/map.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatButtonModule} from '@angular/material/button';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatCardModule} from '@angular/material/card';
import { HttpClientModule } from  '@angular/common/http';
import {MatIconModule} from '@angular/material/icon';
import { CookieService } from 'ngx-cookie-service';
import { LoginComponent } from './login/login.component';
import { MakecityComponent } from './makecity/makecity.component';
import { MakepostComponent } from './makepost/makepost.component';
import { MakegroupComponent } from './makegroup/makegroup.component';
import { StaffComponent } from './staff/staff.component';

@NgModule({
  declarations: [
    AppComponent,
    ComuneComponent,
    HomeComponent,
    MapComponent,
    NotFoundComponent,
    LoginComponent,
    MakecityComponent,
    MakepostComponent,
    MakegroupComponent,
    StaffComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule,
    MatCardModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
