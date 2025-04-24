import { provideHttpClient } from "@angular/common/http";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { AddProteomeComponent } from "./components/add-proteome/add-proteome.component";
import { FooterComponent } from "./components/footer/footer.component";
import { LastExecutionsComponent } from './components/last-executions/last-executions.component';
import { NavbarComponent } from "./components/navbar/navbar.component";
import { RealtimeExecutionsComponent } from "./components/realtime-executions/realtime-executions.component";
import { TopologyComponent } from './components/topology/topology.component';
import { DatabasesComponent } from "./pages/databases/databases.component";
import { LoginComponent } from "./pages/login/login.component";
import { NewComponent } from "./pages/new/new.component";
import { ProfileComponent } from "./pages/profile/profile.component";
import { ResultsComponent } from './pages/results/results.component';
import { UsersComponent } from "./pages/users/users.component";

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
    DatabasesComponent,
    LastExecutionsComponent,
    TopologyComponent,
    ResultsComponent
  ],
  imports: [BrowserModule, AppRoutingModule, FormsModule, ReactiveFormsModule],
  providers: [provideHttpClient()],
  bootstrap: [AppComponent],
})
export class AppModule { }
