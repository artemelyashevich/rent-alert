UPDATE public.notification_type_settings
SET engines    = engines || ',WEBSOCKET',
    updated_at = now()
WHERE starts_with(notification_type, 'ROOMMATE_')
  AND engines NOT LIKE '%WEBSOCKET%';
