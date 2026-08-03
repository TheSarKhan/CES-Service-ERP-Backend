-- V36__add_must_change_password.sql
-- Purpose: yeni istifadəçi admin tərəfindən yaradılanda sistem müvəqqəti parol verir və
-- istifadəçi ilk girişdə onu dəyişməyə məcbur olur. Parolun sıfırlanması da eyni cür işləyir.
--
-- Bayraq istifadəçi öz parolunu təyin edən kimi söndürülür (UserService.changeOwnPassword).
-- Mövcud istifadəçilər üçün FALSE qalır — onların parolu artıq özlərinindir.

ALTER TABLE ces_service.users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN ces_service.users.must_change_password IS
    'TRUE olduqda istifadəçi girişdən sonra parolunu dəyişməyə yönləndirilir.';
