import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { LoginComponent } from "./pages/login/login.component";
import { NewComponent } from "./pages/new/new.component";
import { UsersComponent } from "./pages/users/users.component";
import { AdminGuard } from "./auth/admin.guard";
import { ProfileComponent } from "./pages/profile/profile.component";
import { DatabasesComponent } from "./pages/databases/databases.component";

const routes: Routes = [
  { path: "", redirectTo: "/login", pathMatch: "full" },
  { path: "login", component: LoginComponent },
  { path: "new", component: NewComponent, canActivate: [AdminGuard] },
  { path: 'dbs', component: DatabasesComponent, canActivate: [AdminGuard], data: { role: 'ADMIN' } },
  {
    path: "users",
    component: UsersComponent,
    canActivate: [AdminGuard],
    data: { role: "ADMIN" },
  },
  { path: 'profile', component: ProfileComponent, canActivate: [AdminGuard] },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
