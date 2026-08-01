-- =====================================================================
-- SICOI MOBILE — Script de Atualização do Banco de Dados (Supabase)
-- Execute este script no SQL Editor do seu Supabase Dashboard
-- =====================================================================

-- 1. Adicionar colunas de PIN e Perfil (Role) na tabela user_profiles
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS pin  TEXT DEFAULT '2839',
  ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'Solicitante';

COMMENT ON COLUMN public.user_profiles.pin  IS 'PIN de acesso pessoal (Nota: PIN 2839 é o PIN Global de Administrador)';
COMMENT ON COLUMN public.user_profiles.role IS 'Perfil do cadastro no sistema: "Solicitante", "Técnico" ou "Ambos"';

-- 2. Adicionar colunas de PIN e Perfil na tabela ind_maint_technicians
ALTER TABLE public.ind_maint_technicians
  ADD COLUMN IF NOT EXISTS pin  TEXT DEFAULT '2839',
  ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'Técnico';

COMMENT ON COLUMN public.ind_maint_technicians.pin  IS 'PIN de identificação (2839 libera acesso como Administrador)';
COMMENT ON COLUMN public.ind_maint_technicians.role IS 'Função no módulo de Manutenção: "Técnico", "Solicitante" ou "Ambos"';

-- 3. Atualizar/Inserir técnicos e solicitantes padrão (Exemplo)
INSERT INTO public.ind_maint_technicians (name, status, pin, role)
VALUES 
  ('Rodrigo', 'Ativo', '1001', 'Técnico'),
  ('Luiz', 'Ativo', '1002', 'Técnico'),
  ('Carlos Solicitante', 'Ativo', '2001', 'Solicitante'),
  ('Ana Maria Solicitante', 'Ativo', '2002', 'Solicitante'),
  ('João Silva', 'Ativo', '3001', 'Ambos')
ON CONFLICT (id) DO NOTHING;

-- 4. Permitir que usuários mobile INSERIAM novas Ordens de Serviço (Solicitações de Chamado)
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
