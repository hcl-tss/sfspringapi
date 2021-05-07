import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './routing/app-routing.module';
import {AppComponent} from './app.component';
import {SignInComponent} from './sign-in/sign-in.component';
import {TopNavbarComponent} from './top-navbar/top-navbar.component';
import {HomeComponent} from './home/home.component';
import {ForgotPasswordComponent} from './forgot-password/forgot-password.component';

// import angular materials
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';

// import httpClient
import {HttpClientModule} from '@angular/common/http';
import {DashboardComponent} from './dashboard/dashboard.component';
import {SignUpModule} from './sign-up/sign-up.module';
import {SignUpHomeComponent} from './sign-up/sign-up-home/sign-up-home.component';

@NgModule({
  declarations: [
    AppComponent,
    SignInComponent,
    TopNavbarComponent,
    HomeComponent,
    ForgotPasswordComponent,
    DashboardComponent,
    SignUpHomeComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    FormsModule,
    HttpClientModule,
    ReactiveFormsModule,
    MatCardModule,
    SignUpModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {
}
