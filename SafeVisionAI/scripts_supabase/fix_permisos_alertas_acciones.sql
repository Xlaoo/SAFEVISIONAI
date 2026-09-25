-- ============================================================================
-- SCRIPT DE CORRECCIÓN DE PERMISOS Y RLS EN SUPABASE PARA SAFEVISIONAI
-- ============================================================================
-- Este script soluciona definitivamente el error HTTP 403 (insufficient_privilege)
-- al registrar trabajadores, registrar acciones y actualizar alertas.
-- 
-- Ejecutar en el SQL Editor de tu proyecto en Supabase:
-- https://supabase.com/dashboard/project/bypkhulaxfzudvkavwtc/sql/new
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. TABLA: public.acciones_alerta
-- Causa del 403: Falta de GRANT INSERT y SELECT a authenticated y anon
-- ----------------------------------------------------------------------------
GRANT ALL ON TABLE public.acciones_alerta TO authenticated;
GRANT ALL ON TABLE public.acciones_alerta TO anon;
GRANT ALL ON TABLE public.acciones_alerta TO service_role;

-- Permisos sobre secuencias de ID si usa SERIAL / BIGSERIAL
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S' AND c.relname = 'acciones_alerta_id_seq'
    ) THEN
        GRANT ALL ON SEQUENCE public.acciones_alerta_id_seq TO authenticated;
        GRANT ALL ON SEQUENCE public.acciones_alerta_id_seq TO anon;
        GRANT ALL ON SEQUENCE public.acciones_alerta_id_seq TO service_role;
    END IF;
END $$;

-- Habilitar RLS si no estaba habilitado
ALTER TABLE public.acciones_alerta ENABLE ROW LEVEL SECURITY;

-- Políticas de RLS para acciones_alerta
DROP POLICY IF EXISTS "Permitir insertar acciones de alerta" ON public.acciones_alerta;
CREATE POLICY "Permitir insertar acciones de alerta"
ON public.acciones_alerta
FOR INSERT
TO authenticated, anon
WITH CHECK (true);

DROP POLICY IF EXISTS "Permitir leer acciones de alerta" ON public.acciones_alerta;
CREATE POLICY "Permitir leer acciones de alerta"
ON public.acciones_alerta
FOR SELECT
TO authenticated, anon
USING (true);

DROP POLICY IF EXISTS "Permitir eliminar acciones de alerta" ON public.acciones_alerta;
CREATE POLICY "Permitir eliminar acciones de alerta"
ON public.acciones_alerta
FOR DELETE
TO authenticated, anon
USING (true);


-- ----------------------------------------------------------------------------
-- 2. TABLA: public.alertas
-- Causa del 403: Falta de GRANT UPDATE al rol authenticated para cambiar estado a 'ATENDIDA'
-- ----------------------------------------------------------------------------
GRANT SELECT, UPDATE, DELETE ON TABLE public.alertas TO authenticated;
GRANT SELECT, UPDATE, DELETE ON TABLE public.alertas TO anon;
GRANT ALL ON TABLE public.alertas TO service_role;

-- Políticas de RLS para alertas
ALTER TABLE public.alertas ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Permitir leer alertas" ON public.alertas;
CREATE POLICY "Permitir leer alertas"
ON public.alertas
FOR SELECT
TO authenticated, anon
USING (true);

DROP POLICY IF EXISTS "Permitir actualizar alertas" ON public.alertas;
CREATE POLICY "Permitir actualizar alertas"
ON public.alertas
FOR UPDATE
TO authenticated, anon
USING (true)
WITH CHECK (true);

DROP POLICY IF EXISTS "Permitir eliminar alertas" ON public.alertas;
CREATE POLICY "Permitir eliminar alertas"
ON public.alertas
FOR DELETE
TO authenticated, anon
USING (true);


-- ----------------------------------------------------------------------------
-- 3. TABLA: public.trabajadores
-- Causa del 403: Falta de política INSERT que permita registrar nuevos trabajadores
-- ----------------------------------------------------------------------------
GRANT ALL ON TABLE public.trabajadores TO authenticated;
GRANT ALL ON TABLE public.trabajadores TO anon;
GRANT ALL ON TABLE public.trabajadores TO service_role;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S' AND c.relname = 'trabajadores_id_seq'
    ) THEN
        GRANT ALL ON SEQUENCE public.trabajadores_id_seq TO authenticated;
        GRANT ALL ON SEQUENCE public.trabajadores_id_seq TO anon;
        GRANT ALL ON SEQUENCE public.trabajadores_id_seq TO service_role;
    END IF;
END $$;

ALTER TABLE public.trabajadores ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Permitir leer trabajadores" ON public.trabajadores;
CREATE POLICY "Permitir leer trabajadores"
ON public.trabajadores
FOR SELECT
TO authenticated, anon
USING (true);

DROP POLICY IF EXISTS "Permitir registrar trabajadores" ON public.trabajadores;
CREATE POLICY "Permitir registrar trabajadores"
ON public.trabajadores
FOR INSERT
TO authenticated, anon
WITH CHECK (true);

DROP POLICY IF EXISTS "Permitir actualizar trabajadores" ON public.trabajadores;
CREATE POLICY "Permitir actualizar trabajadores"
ON public.trabajadores
FOR UPDATE
TO authenticated, anon
USING (true)
WITH CHECK (true);
