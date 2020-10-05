SELECT
    TIMESTAMP_MILLIS(u.snapshot_timestamp) AS snapshot,
    STRUCT( u.user_id,
            u.username,
            u.disabled,
            u.creation_time,
            u.first_sign_in_time,
            u.first_registration_completion_time,
            u.last_modified_time) AS account,
    STRUCT( u.given_name,
            u.family_name,
            u.about_you,
            u.area_of_research,
            u.current_position,
            u.demographic_survey_completion_time) AS profile,
    STRUCT(u.contact_email,
           u.street_address_1,
           u.street_address_2,
           u.city,
           u.state,
           u.zip_code,
           u.country,
           u.professional_url) AS contact_info,
    STRUCT( STRUCT(u.compliance_training_completion_time AS completion_time,
                   u.compliance_training_bypass_time AS bypass_time,
                   u.compliance_training_expiration_time AS expiration_time) AS compliance_training,
            STRUCT(data_use_agreement_signed_version AS signed_version,
                   data_use_agreement_completion_time AS completion_time,
                   data_use_agreement_bypass_time AS bypass_time) AS data_use_agreement,
            STRUCT(u.era_commons_completion_time AS completion_time,
                   u.era_commons_bypass_time AS bypass_time) AS era_commons,
            STRUCT(u.two_factor_auth_completion_time AS completion_time,
                   u.two_factor_auth_bypass_time AS bypass_time) AS two_factor_auth,
            u.data_access_level) AS compliance,
    STRUCT(u.free_tier_credits_limit_days_override AS days,
           u.free_tier_credits_limit_dollars_override AS dollars) AS free_tier_credits_limit_override
FROM
    reporting_test.latest_users u
ORDER BY
    u.user_id;
