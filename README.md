# Real-Time Support Desk

A learning project built with Spring Boot, MongoDB, JWT, WebSocket/STOMP, and React + Vite.
Google OAuth/OIDC is **not included**.

## Stack
- Java 21 / Spring Boot 3.5.6
- MongoDB
- Spring Security with `CustomUserDetails` and `CustomUserDetailsService`
- BCrypt password hashing and JWT bearer tokens
- WebSocket/STOMP for ticket messages, typing events, and status broadcasts
- React + Vite frontend
- Postman collection in `docs/`

## Run the backend
1. Start MongoDB locally, or provide `MONGODB_URI` for MongoDB Atlas.
2. Set `JWT_SECRET` to a Base64-encoded random key with at least 32 decoded bytes.
3. Open the `backend` folder in IntelliJ as a Maven project.
4. Run `src/main/java/com/example/serviceApp/ServiceAppApplication.java`.

Generate a secret in PowerShell:
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Set environment variables in IntelliJ Run Configuration:
- `JWT_SECRET`: output from the command above
- `MONGODB_URI`: optional; default is `mongodb://localhost:27017/support_desk`

If you have Maven installed, from `backend`:
```powershell
mvn clean compile
mvn spring-boot:run
```
If the Maven Wrapper files are not present, use Maven installed on your machine or run from IntelliJ.

## Run the frontend
Open a terminal in `frontend`:
```bash
npm install
npm run dev
```
Open the URL Vite prints (normally `http://localhost:5173`).

## First admin and support agent
Register normally; every new account is assigned the `USER` role. To test the other roles, update the `role` field of a user document in MongoDB to `ADMIN` or `SUPPORT_AGENT`. Never accept a role from the public registration request.

## Main REST endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{ticketId}`
- `PATCH /api/tickets/{ticketId}`
- `PATCH /api/tickets/{ticketId}/status`
- `POST /api/tickets/{ticketId}/assign` (admin)
- `GET /api/tickets/{ticketId}/messages`
- `POST /api/tickets/{ticketId}/messages`
- `GET /api/admin/users` (admin)
- `PATCH /api/admin/users/{userId}/role` (admin)

Ticket list query parameters: `page`, `size`, `status`, `priority`, and `search`.

## WebSocket/STOMP
- Connect endpoint: `/ws`
- Subscribe to `/topic/tickets/{ticketId}`
- Send message to `/app/tickets/{ticketId}/message`
- Send typing event to `/app/tickets/{ticketId}/typing`
- The server checks ticket access before accepting ticket-specific STOMP actions.
- This sample uses a simple STOMP CONNECT token header `Authorization: Bearer <JWT>`. Ensure your WebSocket client sends it.

## Security notes
- Do not commit `.env`, JWT secrets, or database credentials.
- This is a student project starter. Review it, understand it, and test every authorization path before deployment.

## Multi-user testing and ticket workflow

- The demo frontend stores the signed-in user and JWT in `sessionStorage`, so each independently opened browser tab can sign in as a different account. Do not duplicate a logged-in tab if your browser clones its session state; open a new tab and navigate to the frontend URL.
- Users create tickets without selecting priority; the backend assigns `MEDIUM` by default.
- Admins can update priority and assignment; the backend saves both to MongoDB. Admins can update status and reopen a closed ticket by setting its status to `OPEN`.
- Assigned support agents can chat on their tickets and update ticket status.
