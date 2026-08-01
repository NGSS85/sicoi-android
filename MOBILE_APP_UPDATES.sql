-- =====================================================================
-- SICOI MOBILE — Script de Atualização do Banco de Dados (Supabase)
-- Execute este script no SQL Editor do seu Supabase Dashboard
-- =====================================================================

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 1: Colunas de PIN e Perfil (já existentes — seguro re-executar)
-- ─────────────────────────────────────────────────────────────────────
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS pin  TEXT DEFAULT '2839',
  ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'Solicitante';

COMMENT ON COLUMN public.user_profiles.pin  IS 'PIN de acesso pessoal (2839 = PIN Global de Administrador)';
COMMENT ON COLUMN public.user_profiles.role IS 'Perfil: "Solicitante", "Técnico" ou "Ambos"';

ALTER TABLE public.ind_maint_technicians
  ADD COLUMN IF NOT EXISTS pin  TEXT DEFAULT '2839',
  ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'Técnico';

COMMENT ON COLUMN public.ind_maint_technicians.pin  IS 'PIN de identificação (2839 = Administrador)';
COMMENT ON COLUMN public.ind_maint_technicians.role IS 'Função: "Técnico", "Solicitante" ou "Ambos"';

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 2: NOVOS CAMPOS — Formulário do Solicitante (Mobile)
-- Necessário para o novo formulário de abertura de O.S. pelo app
-- ─────────────────────────────────────────────────────────────────────

-- 2a. Número do Patrimônio (tag de identificação do ativo)
ALTER TABLE public.ind_maint_os
  ADD COLUMN IF NOT EXISTS numero_patrimonio TEXT DEFAULT NULL;

COMMENT ON COLUMN public.ind_maint_os.numero_patrimonio
  IS 'Número de patrimônio (tag) do equipamento informado pelo solicitante via app mobile';

-- 2b. Tipo de manutenção (Mecânica, Elétrica, Hidráulica, Pneumática)
ALTER TABLE public.ind_maint_os
  ADD COLUMN IF NOT EXISTS tipo_manutencao TEXT DEFAULT NULL;

COMMENT ON COLUMN public.ind_maint_os.tipo_manutencao
  IS 'Tipo(s) de manutenção: "Mecânica", "Elétrica", "Hidráulica", "Pneumática". Múltiplos separados por vírgula.';

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 3: SEQUENCE para Numeração Automática de O.S.
-- O número da OS é sequencial e compartilhado entre web + mobile
-- ─────────────────────────────────────────────────────────────────────

-- Cria a sequence (começa do 1 se não existir)
CREATE SEQUENCE IF NOT EXISTS public.os_numero_seq
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

-- Função RPC chamada pelo app para obter o PRÓXIMO número de OS
-- Retorna formato: "OS-0001"
CREATE OR REPLACE FUNCTION public.get_next_os_number()
RETURNS TEXT
LANGUAGE sql
SECURITY DEFINER
AS $$
  SELECT 'OS-' || LPAD(nextval('public.os_numero_seq')::TEXT, 4, '0');
$$;

-- Permissão de execução
GRANT EXECUTE ON FUNCTION public.get_next_os_number() TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_next_os_number() TO anon;

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 4: Bucket de Fotos do Problema (Supabase Storage)
-- ─────────────────────────────────────────────────────────────────────

-- Cria o bucket "os-attachments"
-- Se preferir, crie via Dashboard > Storage > New Bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('os-attachments', 'os-attachments', true)
ON CONFLICT (id) DO NOTHING;

-- Policy: qualquer usuário autenticado pode fazer upload
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'storage'
      AND tablename  = 'objects'
      AND policyname = 'Authenticated users can upload OS attachments'
  ) THEN
    CREATE POLICY "Authenticated users can upload OS attachments"
      ON storage.objects FOR INSERT
      TO authenticated
      WITH CHECK (bucket_id = 'os-attachments');
  END IF;
END $$;

-- Policy: leitura pública das fotos (para exibir no sistema web)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'storage'
      AND tablename  = 'objects'
      AND policyname = 'Public read OS attachments'
  ) THEN
    CREATE POLICY "Public read OS attachments"
      ON storage.objects FOR SELECT
      TO public
      USING (bucket_id = 'os-attachments');
  END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 5: Policy de INSERT para o app mobile (seguro re-executar)
-- ─────────────────────────────────────────────────────────────────────
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE tablename = 'ind_maint_os'
    AND policyname = 'Mobile approved users can insert OS'
  ) THEN
    CREATE POLICY "Mobile approved users can insert OS"
      ON public.ind_maint_os
      FOR INSERT
      WITH CHECK (
        EXISTS (
          SELECT 1 FROM public.user_profiles
          WHERE id = auth.uid()
            AND approval_status = 'approved'
        )
        OR public.is_admin(auth.uid())
        OR auth.uid() IS NULL
      );
  END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────
-- BLOCO 6 (OPCIONAL): Dados de técnicos/solicitantes de exemplo
-- ─────────────────────────────────────────────────────────────────────
INSERT INTO public.ind_maint_technicians (name, status, pin, role)
VALUES
  ('Rodrigo', 'Ativo', '1001', 'Técnico'),
  ('Luiz',    'Ativo', '1002', 'Técnico'),
  ('Carlos Solicitante', 'Ativo', '2001', 'Solicitante'),
  ('Ana Maria', 'Ativo', '2002', 'Solicitante'),
  ('João Silva', 'Ativo', '3001', 'Ambos')
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────
-- VERIFICAÇÃO FINAL — Execute separado após rodar o script acima
-- Confirma que as colunas foram criadas corretamente
-- ─────────────────────────────────────────────────────────────────────
-- SELECT column_name, data_type, column_default
-- FROM information_schema.columns
-- WHERE table_schema = 'public'
--   AND table_name   = 'ind_maint_os'
-- ORDER BY ordinal_position;
