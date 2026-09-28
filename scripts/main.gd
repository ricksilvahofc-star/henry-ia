extends Node2D

# Henry Western - mapa aberto expandido
# Mundo 3200x2200 com cidade, estrada, rio, montanhas, fazenda, mina e acampamento.

const WORLD_SIZE := Vector2(3200, 2200)
var player_pos := Vector2(640, 700)
var velocity := Vector2.ZERO
var speed := 260.0
var bullets: Array[Dictionary] = []
var enemies: Array[Dictionary] = [
    {"pos": Vector2(1780, 620), "alive": true},
    {"pos": Vector2(2050, 900), "alive": true},
    {"pos": Vector2(2650, 1450), "alive": true},
    {"pos": Vector2(2320, 1730), "alive": true}
]
var money := 25
var wanted := 0
var health := 100
var gameplay := preload("res://scripts/gameplay_system.gd").new()
var touch_move := Vector2.ZERO
var touch_fire := Vector2.ZERO
var touch_moving := false
var touch_firing := false

var ground_texture: Texture2D = preload("res://assets/generated/desert_ground.svg")
var wood_texture: Texture2D = preload("res://assets/generated/wood_planks.svg")

func _ready() -> void:
    add_child(gameplay)
    var camera := Camera2D.new()
    camera.position = player_pos
    camera.position_smoothing_enabled = true
    camera.position_smoothing_speed = 6.0
    camera.limit_left = 0
    camera.limit_top = 0
    camera.limit_right = int(WORLD_SIZE.x)
    camera.limit_bottom = int(WORLD_SIZE.y)
    add_child(camera)
    camera.make_current()
    queue_redraw()

func _input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        if event.pressed:
            if event.position.x < get_viewport_rect().size.x * 0.5:
                touch_move = get_global_mouse_position()
                touch_moving = true
            else:
                touch_fire = get_global_mouse_position()
                touch_firing = true
                fire_toward(touch_fire)
        else:
            if event.position.x < get_viewport_rect().size.x * 0.5:
                touch_moving = false
                touch_move = Vector2.ZERO
            else:
                touch_firing = false
                touch_fire = Vector2.ZERO
    elif event is InputEventScreenDrag:
        if event.position.x < get_viewport_rect().size.x * 0.5:
            touch_move = get_global_mouse_position()
        else:
            touch_fire = get_global_mouse_position()
            if event.relative.length() > 10.0:
                fire_toward(touch_fire)

func fire_toward(target: Vector2) -> void:
    var dir := (target - player_pos).normalized()
    if dir.length() > 0.0:
        bullets.append({"pos": player_pos, "vel": dir * 650.0})

func _process(delta: float) -> void:
    var direction := Input.get_vector("move_left", "move_right", "move_up", "move_down")
    if touch_moving:
        direction = (touch_move - player_pos).normalized()
    velocity = direction * gameplay.get_speed(speed)
    player_pos += velocity * delta
    player_pos.x = clamp(player_pos.x, 60.0, WORLD_SIZE.x - 60.0)
    player_pos.y = clamp(player_pos.y, 180.0, WORLD_SIZE.y - 60.0)

    var camera := get_viewport().get_camera_2d()
    if camera:
        camera.position = player_pos

    if Input.is_action_just_pressed("interact"):
        gameplay.interact(player_pos)
        if gameplay.mission_state == "completed":
            money += gameplay.mission_reward
            gameplay.mission_state = "rewarded"
            gameplay.interaction_message = "Recompensa recebida! Explore o território."

    if Input.is_action_just_pressed("fire"):
        fire_toward(get_global_mouse_position())

    for bullet in bullets:
        bullet.pos += bullet.vel * delta
    bullets = bullets.filter(func(b): return Rect2(Vector2.ZERO, WORLD_SIZE).has_point(b.pos))

    for enemy in enemies:
        if not enemy.alive:
            continue
        for bullet in bullets:
            if bullet.pos.distance_to(enemy.pos) < 28.0:
                enemy.alive = false
                money += 10
                wanted = min(wanted + 1, 5)

    queue_redraw()

func _draw() -> void:
    # Céu e terreno de um mundo muito maior que a tela.
    draw_rect(Rect2(0, 0, WORLD_SIZE.x, WORLD_SIZE.y), Color("#8fc5df"))
    draw_rect(Rect2(0, 180, WORLD_SIZE.x, WORLD_SIZE.y - 180), Color("#a8753f"))
    draw_texture_rect(ground_texture, Rect2(0, 180, WORLD_SIZE.x, WORLD_SIZE.y - 180), true)

    # Montanhas do norte e do oeste.
    for p in [Vector2(150,500), Vector2(520,420), Vector2(900,510), Vector2(1250,390), Vector2(1580,500), Vector2(2850,520)]:
        draw_polygon(PackedVector2Array([p + Vector2(-230,260), p + Vector2(0,-180), p + Vector2(250,260)]), PackedColorArray([Color("#76502f")]))

    # Rio atravessando o mapa.
    var river := PackedVector2Array([
        Vector2(1450,180), Vector2(1570,450), Vector2(1490,760), Vector2(1650,1050),
        Vector2(1530,1380), Vector2(1700,1700), Vector2(1600,2200),
        Vector2(1840,2200), Vector2(1930,1720), Vector2(1770,1380), Vector2(1890,1040),
        Vector2(1730,740), Vector2(1810,430), Vector2(1690,180)
    ])
    draw_colored_polygon(river, Color("#3c82a0"))

    # Estrada principal e ramificações.
    draw_line(Vector2(100,700), Vector2(900,760), Color("#d2a06a"), 75)
    draw_line(Vector2(900,760), Vector2(1320,1050), Color("#d2a06a"), 65)
    draw_line(Vector2(1320,1050), Vector2(2150,1250), Color("#d2a06a"), 70)
    draw_line(Vector2(2150,1250), Vector2(3050,1480), Color("#d2a06a"), 65)
    draw_line(Vector2(2150,1250), Vector2(2400,650), Color("#d2a06a"), 55)
    draw_line(Vector2(2150,1250), Vector2(2550,1850), Color("#d2a06a"), 55)

    # Dusty Creek - cidade inicial.
    draw_rect(Rect2(300, 560, 720, 420), Color("#71472a"))
    draw_rect(Rect2(340, 600, 160, 120), Color("#b47b45"))
    draw_rect(Rect2(550, 590, 190, 150), Color("#c58b52"))
    draw_rect(Rect2(790, 610, 170, 130), Color("#b47b45"))
    draw_rect(Rect2(430, 790, 220, 120), Color("#c08a4c"))
    draw_rect(Rect2(700, 790, 230, 120), Color("#9f6c3d"))
    draw_string(ThemeDB.fallback_font, Vector2(390,535), "DUSTY CREEK", HORIZONTAL_ALIGNMENT_LEFT, -1, 32, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(350,1015), "SAL00N   XERIFADO   LOJA", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)

    # Fazenda e celeiros.
    draw_rect(Rect2(1020, 1180, 460, 330), Color("#7b5a2f"))
    draw_rect(Rect2(1080, 1230, 150, 120), Color("#a64f32"))
    draw_rect(Rect2(1280, 1260, 120, 90), Color("#a64f32"))
    draw_circle(Vector2(1170,1430), 55, Color("#4f7a3d"))
    draw_circle(Vector2(1330,1420), 65, Color("#4f7a3d"))
    draw_string(ThemeDB.fallback_font, Vector2(1060,1160), "RANCHO RED ROCK", HORIZONTAL_ALIGNMENT_LEFT, -1, 25, Color.WHITE)

    # Mina abandonada.
    draw_rect(Rect2(2050, 1650, 360, 260), Color("#5c4634"))
    draw_rect(Rect2(2130, 1570, 210, 120), Color("#6e472d"))
    draw_string(ThemeDB.fallback_font, Vector2(2100,1940), "MINA BLACK RIDGE", HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color.WHITE)

    # Acampamento dos bandidos.
    draw_circle(Vector2(2650,1450), 150, Color("#69442e"))
    draw_colored_polygon(PackedVector2Array([Vector2(2570,1400),Vector2(2650,1280),Vector2(2730,1400)]), Color("#8d5c38"))
    draw_colored_polygon(PackedVector2Array([Vector2(2720,1480),Vector2(2800,1360),Vector2(2870,1480)]), Color("#8d5c38"))
    draw_string(ThemeDB.fallback_font, Vector2(2520,1640), "ACAMPAMENTO", HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color.WHITE)

    # NPCs e cavalo da missão inicial.
    draw_circle(gameplay.npc_pos, 18, Color("#31536a"))
    draw_circle(gameplay.npc_pos + Vector2(0,-15), 11, Color("#bd7f59"))
    draw_string(ThemeDB.fallback_font, gameplay.npc_pos + Vector2(-30,-30), "XERIFE", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color.WHITE)
    draw_ellipse(gameplay.horse_pos, Vector2(34,18), Color("#5b3823"))
    draw_circle(gameplay.horse_pos + Vector2(28,-18), 13, Color("#5b3823"))
    draw_string(ThemeDB.fallback_font, gameplay.horse_pos + Vector2(-30,45), "CAVALO", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color.WHITE)

    if gameplay.mission_state == "accepted":
        draw_circle(gameplay.target_pos, 20, Color("#7d2424"))
        draw_circle(gameplay.target_pos + Vector2(0,-16), 11, Color("#bd7f59"))
        draw_string(ThemeDB.fallback_font, gameplay.target_pos + Vector2(-30,-30), "ALVO", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("#ffd98a"))

    # Jogador, inimigos e projéteis.
    draw_circle(player_pos, 20, Color("#2e2520"))
    draw_circle(player_pos + Vector2(0,-18), 13, Color("#d39a6c"))
    draw_rect(Rect2(player_pos.x-18, player_pos.y-35, 36, 7), Color("#4a2e1d"))
    draw_line(player_pos, get_global_mouse_position(), Color(1,1,1,0.25), 2)

    for enemy in enemies:
        if enemy.alive:
            draw_circle(enemy.pos, 18, Color("#6d2525"))
            draw_circle(enemy.pos + Vector2(0,-15), 11, Color("#bd7f59"))
    for bullet in bullets:
        draw_circle(bullet.pos, 5, Color("#f8e7a1"))

    # HUD acompanha a câmera porque fica na camada do mundo nesta primeira versão.
    draw_rect(Rect2(player_pos.x-600, player_pos.y-330, 520, 112), Color(0,0,0,0.62))
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(-585,-302), "HENRY WESTERN", HORIZONTAL_ALIGNMENT_LEFT, -1, 25, Color.WHITE)
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(-585,-275), "Vida: %d   $%d   Procurado: %d/5" % [health, money, wanted], HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(-585,-248), gameplay.get_mission_label(), HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color("#ffd98a"))
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(-585,310), "CELULAR: esquerda = mover | direita = atirar", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(60,280), gameplay.interaction_message, HORIZONTAL_ALIGNMENT_LEFT, 540, 18, Color.WHITE)
    draw_circle(player_pos + Vector2(500,260), 55, Color(0,0,0,0.22))
    draw_string(ThemeDB.fallback_font, player_pos + Vector2(470,267), "FIRE", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)

    if touch_moving:
        draw_circle(touch_move, 42, Color(1,1,1,0.18))

func draw_ellipse(center: Vector2, radii: Vector2, color: Color) -> void:
    var points := PackedVector2Array()
    for i in range(24):
        var a := TAU * float(i) / 24.0
        points.append(center + Vector2(cos(a) * radii.x, sin(a) * radii.y))
    draw_colored_polygon(points, color)
