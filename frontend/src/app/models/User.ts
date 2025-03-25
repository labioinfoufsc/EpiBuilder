import { EpitopeTaskData } from "./EpitopeTaskData";

/**
 * Represents a user within the system, including their details and execution history.
 *
 * This model stores essential information about a user, including their unique 
 * identifier, username, name, password, role, and a history of their last executions.
 * It is used to manage user data, authenticate access, assign roles, and track previous 
 * tasks or processes that the user has executed.
 *
 * Properties:
 * - `id`: The unique identifier for the user.
 * - `username`: The username chosen by the user for login and identification.
 * - `name`: The full name of the user.
 * - `password`: The password used for authenticating the user’s login.
 * - `role`: The role or permission level assigned to the user (e.g., admin, user, etc.).
 * - `lastExecutions`: An optional array of `EpitopeTaskData` instances representing the 
 *   user’s recent executions or tasks performed within the system.
 * - `token`: An optional token used for authentication and authorization purposes.
 *
 * This class is essential for managing user accounts, securing authentication, 
 * handling user-specific access, and tracking past executions or actions within the system.
 */
export class User {
   id?: number;
   username!: string;
   name!: string;
   password!: string;
   role!: string;
   lastExecutions?: EpitopeTaskData[];
   token?: string;

   constructor(name: string, username: string, password: string, role: string, lastExecutions?: EpitopeTaskData[], token?: string) {
      this.name = name;
      this.username = username;
      this.password = password;
      this.role = role;
      this.lastExecutions = lastExecutions;
      this.token = token;
   }
}
