import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { LoginComponent } from "./pages/login/login.component";
import { NavbarComponent } from "./components/navbar/navbar.component";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { FooterComponent } from "./components/footer/footer.component";
import { provideHttpClient } from "@angular/common/http";
import { UsersComponent } from "./pages/users/users.component";
import { NewComponent } from "./pages/new/new.component";
import { AddProteomeComponent } from "./components/add-proteome/add-proteome.component";
import { RealtimeExecutionsComponent } from "./components/realtime-executions/realtime-executions.component";
import { ProfileComponent } from "./pages/profile/profile.component";
import { DatabasesComponent } from "./pages/databases/databases.component";

@NgModule({
  declarations: [
    AppComponent,
    FooterComponent,
    NavbarComponent,
    LoginComponent,
    UsersComponent,
    NewComponent,
    AddProteomeComponent,
    RealtimeExecutionsComponent,
    ProfileComponent,
    DatabasesComponent
  ],
  imports: [BrowserModule, AppRoutingModule, FormsModule, ReactiveFormsModule],
  providers: [provideHttpClient()],
  bootstrap: [AppComponent],
})
export class AppModule {}
