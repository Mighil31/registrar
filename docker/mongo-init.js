// Runs only when the volume is first created: changing .env later doesn't change this user.
// The app gets readWrite on its own database only; the root user is for admin and the replica set.
const dbName = process.env.MONGO_APP_DB;
db.getSiblingDB(dbName).createUser({
  user: process.env.MONGO_APP_USER,
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [{ role: "readWrite", db: dbName }],
});
