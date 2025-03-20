/**
 *
 * Represents a user within the system, including their details and execution history.
 *
 * This model stores essential information about a user, including their unique
 * identifier, username, name, password, role and token.
 * It is used to manage user data, authenticate access, assign roles, and track previous
 * tasks or processes that the user has executed.
 *
 * Properties:
 * - `id`: The unique identifier for the user.
 * - `username`: The username chosen by the user for login and identification.
 * - `name`: The full name of the user.
 * - `password`: The password used for authenticating the user’s login.
 * - `role`: The role or permission level assigned to the user (e.g., admin, user, etc.).
 * - `token`: The authentication token used to validate the user’s session and access.
 *
 */
export class User {
  id?: number;
  username!: string;
  name!: string;
  password?: string;
  role!: string;
  token?: string;

  constructor(
    name: string,
    username: string,
    password: string,
    role: string,
    token: string
  ) {
    this.name = name;
    this.username = username;
    this.password = password;
    this.role = role;
    this.token = token;
  }
}
