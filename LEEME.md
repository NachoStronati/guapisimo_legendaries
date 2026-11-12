# Guapisimo Legendaries (Fabric 1.21.1 + Cobblemon)

Altares para invocar legendarios. Clic derecho en el altar con su ofrenda ->
aparece el legendario encima y el altar se apaga para siempre.

## Estructuras
- Templo de Tapu Fini: en cualquier océano, sobre la superficie del agua.
  - Archivo: data/guapisimo/structure/tapu_fini.nbt
  - Biomas: data/guapisimo/tags/worldgen/biome/has_structure/templo_guardian_poni.json
  - Rareza: data/guapisimo/worldgen/structure_set/templo_guardian_poni.json
    (spacing = distancia media en chunks; separation = distancia mínima; más alto = más raro)
  - Altura: worldgen/structure/templo_guardian_poni.json -> "start_height" (-1 = suelo al nivel del agua)

## Probar
/locate structure guapisimo:templo_guardian_poni   (en un mundo NUEVO)

## Compilar
.\gradlew.bat build  ->  build\libs\guapisimo_legendaries-1.0.0.jar
