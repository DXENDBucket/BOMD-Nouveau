"""Verify glyph recipes, native references, licensing and artifact isolation."""
from pathlib import Path
from zipfile import ZipFile
import json
import struct
import sys

root = Path(__file__).resolve().parents[1]
version = (root / 'VERSION').read_text(encoding='utf-8').strip()
forge = '--forge' in sys.argv
build_root = root / 'forge' if forge else root
resources = root / 'common/src/main/resources'
languages = {p.stem: json.loads(p.read_text(encoding='utf-8'))
             for p in (resources / 'assets/bomd_nouveau/lang').glob('*.json')}
assert set(languages) == {'en_us', 'zh_cn'}
assert languages['en_us'].keys() == languages['zh_cn'].keys()
with ZipFile(root / ('forge/libs/bomd-forge1201.jar' if forge else 'libs/bomd.jar')) as native:
    for model in (resources / 'assets/bomd_nouveau/models/item').glob('*.json'):
        definition = json.loads(model.read_text(encoding='utf-8'))
        namespace, path = definition['parent'].split(':')
        assert f'assets/{namespace}/models/{path}.json' in native.namelist()
        for language in languages.values():
            assert language[f'bomd_nouveau.glyph_name.{model.stem}']
            assert language[f'bomd_nouveau.glyph_desc.{model.stem}']
recipes = list((resources / 'data/bomd_nouveau/recipe').glob('glyph_*.json'))
assert len(recipes) == 11
binary = build_root / f'build/libs/bomd-nouveau-{version}-{"mc1.20.1-forge" if forge else "mc1.21.1-neoforge"}.jar'
source = build_root / f'build/libs/bomd-nouveau-{version}-sources.jar'
for artifact in (binary, source):
    with ZipFile(artifact) as jar:
        names = jar.namelist()
        for notice in ('LICENSE', 'licenses/GPL-3.0.txt', 'NOTICE', 'THIRD_PARTY_NOTICES.md'):
            assert notice in names, (artifact, notice)
        assert b'GNU LESSER GENERAL PUBLIC LICENSE' in jar.read('LICENSE')
        assert not any(n.endswith('.jar') for n in names), 'Dependencies must remain separate'
        assert not any('GameTests' in n or 'test_marker' in n for n in names)
        for recipe in recipes:
            path = recipe.relative_to(resources).as_posix()
            expected = json.loads(recipe.read_text(encoding='utf-8'))
            if forge:
                assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 15
                path = path.replace('/recipe/', '/recipes/')
                expected = {'type': expected['type'], 'exp': expected['exp'],
                            'output': expected['output']['id'], 'count': expected['output']['count'],
                            'inputItems': [{'item': item} for item in expected['inputs']]}
            assert json.loads(jar.read(path)) == expected
        if artifact == binary:
            metadata = 'META-INF/mods.toml' if forge else 'META-INF/neoforge.mods.toml'
            assert version in jar.read(metadata).decode()
            assert 'LGPL-3.0-only' in jar.read(metadata).decode()
            for name in names:
                if name.endswith('.class'):
                    assert name.startswith('dev/bomdnouveau/'), f'Unexpected bundled class: {name}'
                    assert struct.unpack('>H', jar.read(name)[6:8])[0] == (61 if forge else 65)
            mixins = json.loads(jar.read('bomd_nouveau.mixins.json'))
            for mixin in mixins['mixins']:
                assert f'dev/bomdnouveau/mixin/{mixin}.class' in names
            if forge:
                assert mixins['compatibilityLevel'] == 'JAVA_17'
                assert mixins['refmap'] in names
                mappings = json.loads(jar.read(mixins['refmap']))
                assert mappings['mappings'], 'Release refmap must contain real Minecraft mappings'
                assert not any('/recipe/' in n for n in names)
        else:
            assert 'dev/bomdnouveau/casting/NativePayloads.java' in names
if '--artifacts-only' in sys.argv:
    print(f'{version}: 11 glyph recipes, native icon references, localizations, licensing and source/binary isolation verified; runtime tests not rerun.')
    sys.exit(0)
log = (build_root / 'build/payload-tests.log').read_text(encoding='utf-16')
assert 'All 6 required tests passed' in log and 'BUILD SUCCESSFUL' in log
print(f'{version}: 11 glyph recipes, native icon references, both localizations, LGPL/GPL notices, source/binary isolation and 6 focused GameTests verified.')
