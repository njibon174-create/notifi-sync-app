# Notification Sync Web Dashboard

A small React + Vite dashboard for viewing synced SMS/app notifications from the Supabase backend.

## Folder

`web-dashboard/`

## Environment variables

Create a `.env` file locally with:

```bash
VITE_SUPABASE_URL=https://epqakuroqjtrhhcyweoa.supabase.co
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key
```

For Vercel, set the same variables in the project settings.

## Local development

```bash
cd web-dashboard
npm install
npm run dev
```

## Build

```bash
npm run build
```

## Deployment on Vercel

This project is ready for a static Vercel deployment.

1. Connect the GitHub repo in Vercel
2. Set the root directory to `web-dashboard`
3. Add environment variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
4. Use the default build command: `npm run build`
5. Use the default output directory from Vite: `dist`

`vercel.json` is included to support client-side routing fallback.

## Features

- Supabase email/password login
- Persisted session via Supabase localStorage auth
- Notification list sorted newest first
- Filter by device, type, and text search
- Click a notification to mark it read
- Realtime subscription for new notifications
- Load-more pagination
