#!/usr/bin/env python3
"""Le os relatorios XML do Maven Surefire e imprime um resumo em Markdown.

Uso: python3 scripts/resumo_testes.py target/surefire-reports
No pipeline a saida vai para o resumo da execucao ($GITHUB_STEP_SUMMARY).
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    pasta = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
    arquivos = sorted(glob.glob(os.path.join(pasta, "TEST-*.xml")))

    print("### Testes automatizados\n")
    if not arquivos:
        print("> Nenhum relatorio de teste encontrado: a execucao parou antes dos testes "
              "(provavel erro de compilacao). Veja o log da etapa **Build e testes**.")
        return 0

    total = falhas = erros = ignorados = 0
    tempo = 0.0
    linhas = []
    problemas = []

    for arq in arquivos:
        suite = ET.parse(arq).getroot()
        t = int(suite.get("tests", 0))
        f = int(suite.get("failures", 0))
        e = int(suite.get("errors", 0))
        s = int(suite.get("skipped", 0))
        total, falhas, erros, ignorados = total + t, falhas + f, erros + e, ignorados + s
        tempo += float(suite.get("time", 0) or 0)
        nome = suite.get("name", os.path.basename(arq)).rsplit(".", 1)[-1]
        situacao = "OK" if f + e == 0 else "FALHOU"
        linhas.append(f"| {nome} | {t} | {f + e} | {s} | {situacao} |")

        for caso in suite.iter("testcase"):
            for tipo in ("failure", "error"):
                no = caso.find(tipo)
                if no is not None:
                    msg = (no.get("message") or no.get("type") or "").strip().splitlines()
                    msg = msg[0][:200] if msg else ""
                    problemas.append(f"- `{nome}.{caso.get('name')}`: {msg}")

    situacao_geral = "Todos os testes passaram" if falhas + erros == 0 else "Ha testes com falha"
    print(f"**{situacao_geral}** - {total} executados, {falhas + erros} com falha/erro, "
          f"{ignorados} ignorados, {tempo:.1f}s\n")
    print("| Classe de teste | Testes | Falhas/erros | Ignorados | Situacao |")
    print("|---|---|---|---|---|")
    print("\n".join(linhas))

    if problemas:
        print("\n#### Testes que falharam\n")
        print("\n".join(problemas))
        print("\nO detalhe completo esta no artefato **relatorio-testes-<commit>** desta execucao.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
