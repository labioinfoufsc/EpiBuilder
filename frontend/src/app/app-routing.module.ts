import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { LoginComponent } from "./pages/login/login.component";
import { NewComponent } from "./pages/new/new.component";
import { UsersComponent } from "./pages/users/users.component";
import { AdminGuard } from "./auth/admin.guard";

const routes: Routes = [
  { path: "", redirectTo: "/login", pathMatch: "full" },
  { path: "login", component: LoginComponent },
  { path: "new", component: NewComponent, canActivate: [AdminGuard] },
  {
    path: "users",
    component: UsersComponent,
    canActivate: [AdminGuard],
    data: { role: "ADMIN" },
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
