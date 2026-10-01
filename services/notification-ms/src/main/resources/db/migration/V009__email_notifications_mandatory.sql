UPDATE public.notification_type_settings
SET mandatory  = true,
    updated_at = now()
WHERE notification_type IN ('EMAIL_ACTIVATION', 'EMAIL_ACTIVATION_SUCCESS');
