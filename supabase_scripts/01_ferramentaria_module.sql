-- ==============================================================================
-- SICOI 26 - MÓDULO DE FERRAMENTARIA
-- Script de Migração do Banco de Dados Supabase (PostgreSQL)
-- ==============================================================================

-- 1. Criação da Tabela de Ordens de Serviço da Ferramentaria
CREATE TABLE IF NOT EXISTS public.tool_maint_os (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_os TEXT,
    codigo_ferramenta TEXT NOT NULL,
    nome_ferramenta TEXT NOT NULL,
    tipo_ferramenta TEXT DEFAULT 'Molde de Injeção',
    maquina_operacao TEXT,
    setor TEXT DEFAULT 'Ferramentaria',
    solicitante TEXT,
    tecnico_responsavel TEXT,
    descricao_problema TEXT NOT NULL,
    prioridade TEXT DEFAULT 'Normal',
    status TEXT DEFAULT 'Aberta', -- 'Aberta', 'Em Execução', 'Pausada', 'Concluída', 'Cancelada'
    solucao_aplicada TEXT,
    pecas_utilizadas TEXT,
    tempo_gasto TEXT,
    data_abertura TIMESTAMPTZ DEFAULT now(),
    data_fim TIMESTAMPTZ,
    foto_antes_url TEXT,
    foto_depois_url TEXT,
    assinatura_url TEXT,
    motivo_pausa TEXT,
    observacoes_pausa TEXT,
    atualizacao TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Índices para consultas rápidas
CREATE INDEX IF NOT EXISTS idx_tool_maint_os_status ON public.tool_maint_os(status);
CREATE INDEX IF NOT EXISTS idx_tool_maint_os_tecnico ON public.tool_maint_os(tecnico_responsavel);
CREATE INDEX IF NOT EXISTS idx_tool_maint_os_codigo ON public.tool_maint_os(codigo_ferramenta);
CREATE INDEX IF NOT EXISTS idx_tool_maint_os_data ON public.tool_maint_os(data_abertura DESC);

-- 2. Trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION public.handle_tool_maint_os_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_tool_maint_os_updated_at ON public.tool_maint_os;
CREATE TRIGGER trigger_tool_maint_os_updated_at
    BEFORE UPDATE ON public.tool_maint_os
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_tool_maint_os_updated_at();

-- 3. Função para Gerar o Próximo Número Sequencial da OS (ex: FER-001/26)
CREATE OR REPLACE FUNCTION public.generate_tool_os_number()
RETURNS TRIGGER AS $$
DECLARE
    current_year_str TEXT;
    next_seq INT;
BEGIN
    IF NEW.numero_os IS NULL OR NEW.numero_os = '' THEN
        current_year_str := to_char(now(), 'YY');
        
        -- Busca o maior sequencial para o ano corrente
        SELECT COALESCE(MAX(
            NULLIF(
                regexp_replace(
                    split_part(split_part(numero_os, '/', 1), 'FER-', 2),
                    '[^0-9]', '', 'g'
                ),
                ''
            )::INT
        ), 0) + 1
        INTO next_seq
        FROM public.tool_maint_os
        WHERE numero_os LIKE '%/' || current_year_str;

        NEW.numero_os := 'FER-' || lpad(next_seq::TEXT, 3, '0') || '/' || current_year_str;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_generate_tool_os_number ON public.tool_maint_os;
CREATE TRIGGER trigger_generate_tool_os_number
    BEFORE INSERT ON public.tool_maint_os
    FOR EACH ROW
    EXECUTE FUNCTION public.generate_tool_os_number();

-- 4. Função RPC para buscar O.S. Abertas de Ferramentaria por Técnico
CREATE OR REPLACE FUNCTION public.get_open_tool_os_by_technician(p_technician_name TEXT)
RETURNS SETOF public.tool_maint_os AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM public.tool_maint_os
    WHERE lower(status) NOT IN ('concluída', 'concluida', 'finalizada', 'cancelada')
      AND (
          p_technician_name IS NULL
          OR p_technician_name = ''
          OR lower(tecnico_responsavel) = lower(p_technician_name)
          OR tecnico_responsavel IS NULL
          OR tecnico_responsavel = ''
          OR lower(tecnico_responsavel) IN ('não atribuído', 'nao atribuido', 'aguardando técnico')
      )
    ORDER BY data_abertura DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Habilitar Row Level Security (RLS) e Políticas de Acesso
ALTER TABLE public.tool_maint_os ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Permitir leitura de OS de Ferramentaria" ON public.tool_maint_os;
CREATE POLICY "Permitir leitura de OS de Ferramentaria"
    ON public.tool_maint_os FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Permitir inserção de OS de Ferramentaria" ON public.tool_maint_os;
CREATE POLICY "Permitir inserção de OS de Ferramentaria"
    ON public.tool_maint_os FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

DROP POLICY IF EXISTS "Permitir atualização de OS de Ferramentaria" ON public.tool_maint_os;
CREATE POLICY "Permitir atualização de OS de Ferramentaria"
    ON public.tool_maint_os FOR UPDATE
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- Permissões de grants para roles públicas e autenticadas
GRANT ALL ON public.tool_maint_os TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.get_open_tool_os_by_technician(TEXT) TO anon, authenticated, service_role;

-- 6. Liberação de Acesso no Perfil dos Usuários (user_profiles)
-- Adiciona o módulo 'ferramentaria' na lista allowed_modules dos perfis que devem ter acesso:
-- (Ajuste o WHERE conforme a sua regra de negócio para liberar para técnicos/solicitantes específicos)

UPDATE public.user_profiles
SET allowed_modules = array_append(allowed_modules, 'ferramentaria')
WHERE NOT ('ferramentaria' = ANY(COALESCE(allowed_modules, ARRAY[]::text[])))
  AND (role IN ('Técnico', 'Ambos', 'Admin') OR email ILIKE '%admin%');

-- Se desejar liberar para TODOS os usuários ativos:
-- UPDATE public.user_profiles
-- SET allowed_modules = array_append(allowed_modules, 'ferramentaria')
-- WHERE NOT ('ferramentaria' = ANY(COALESCE(allowed_modules, ARRAY[]::text[])));
