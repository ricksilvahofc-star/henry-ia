extends Node2D

var player_pos := Vector2(640, 420)
var velocity := Vector2.ZERO
var speed := 260.0
var bullets: Array[Dictionary] = []
var enemies: Array[Dictionary] = [
    {"pos": Vector2(900, 260), "alive": true},
    {"pos": Vector2(1030, 500), "alive": true}
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
    queue_redraw()

func _input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        if event.pressed:
            if event.position.x < 640.0:
                touch_move = event.position
                touch_moving = true
            else:
                touch_fire = event.position
                touch_firing = true
                fire_toward(touch_fire)
        else:
            if event.position.x < 640.0:
                touch_moving = false
                touch_move = Vector2.ZERO
            else:
                touch_firing = false
                touch_fire = Vector2.ZERO
    elif event is InputEventScreenDrag:
        if event.position.x < 640.0:
            touch_move = event.position
        else:
            touch_fire = event.position
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
    player_pos.x = clamp(player_pos.x, 50.0, 1230.0)
    player_pos.y = clamp(player_pos.y, 120.0, 670.0)

    if Input.is_action_just_pressed("interact"):
        gameplay.interact(player_pos)
        if gameplay.mission_state == "completed":
            money += gameplay.mission_reward
            gameplay.mission_state = "rewarded"
            gameplay.interaction_message = "Recompensa recebida! Procure outra missão."

    if Input.is_action_just_pressed("fire"):
        fire_toward(get_global_mouse_position())

    for bullet in bullets:
        bullet.pos += bullet.vel * delta
    bullets = bullets.filter(func(b): return Rect2(0, 0, 1280, 720).has_point(b.pos))

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
    draw_rect(Rect2(0, 0, 1280, 720), Color("#8fc5df"))
    draw_rect(Rect2(0, 170, 1280, 550), Color("#a8753f"))
    draw_texture_rect(ground_texture, Rect2(0, 170, 1280, 550), true)
    draw_circle(Vector2(1080, 90), 45, Color("#f6d67a"))

    draw_polygon(PackedVector2Array([Vector2(0,520),Vector2(260,300),Vector2(520,520)]), PackedColorArray([Color("#76502f")]))
    draw_polygon(PackedVector2Array([Vector2(620,520),Vector2(860,330),Vector2(1100,520)]), PackedColorArray([Color("#76502f")]))
    draw_rect(Rect2(540, 170, 120, 550), Color(0.35, 0.24, 0.14, 0.45))

    draw_texture_rect(wood_texture, Rect2(70, 330, 260, 160), true)
    draw_rect(Rect2(70, 330, 260, 160), Color(0.45, 0.27, 0.15, 0.35))
    draw_rect(Rect2(105, 360, 80, 70), Color("#d4b06a"))
    draw_rect(Rect2(215, 350, 85, 80), Color("#c58b52"))
    draw_string(ThemeDB.fallback_font, Vector2(115, 315), "DUSTY CREEK", HORIZONTAL_ALIGNMENT_LEFT, -1, 24, Color.WHITE)
    draw_circle(gameplay.npc_pos, 18, Color("#31536a"))
    draw_circle(gameplay.npc_pos + Vector2(0,-15), 11, Color("#bd7f59"))
    draw_string(ThemeDB.fallback_font, gameplay.npc_pos + Vector2(-30,-30), "XERIFE", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color.WHITE)

    draw_ellipse(gameplay.horse_pos, Vector2(34, 18), Color("#5b3823"))
    draw_circle(gameplay.horse_pos + Vector2(28,-18), 13, Color("#5b3823"))
    draw_string(ThemeDB.fallback_font, gameplay.horse_pos + Vector2(-30,45), "CAVALO", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color.WHITE)

    if gameplay.mission_state == "accepted":
        draw_circle(gameplay.target_pos, 20, Color("#7d2424"))
        draw_circle(gameplay.target_pos + Vector2(0,-16), 11, Color("#bd7f59"))
        draw_string(ThemeDB.fallback_font, gameplay.target_pos + Vector2(-30,-30), "ALVO", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("#ffd98a"))

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

    draw_rect(Rect2(20, 20, 520, 112), Color(0,0,0,0.62))
    draw_string(ThemeDB.fallback_font, Vector2(35, 48), "HENRY WESTERN", HORIZONTAL_ALIGNMENT_LEFT, -1, 25, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(35, 75), "Vida: %d   $%d   Procurado: %d/5" % [health, money, wanted], HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(35, 102), gameplay.get_mission_label(), HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color("#ffd98a"))
    draw_string(ThemeDB.fallback_font, Vector2(35, 690), "CELULAR: esquerda = mover | direita = atirar", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)
    draw_string(ThemeDB.fallback_font, Vector2(700, 650), gameplay.interaction_message, HORIZONTAL_ALIGNMENT_LEFT, 540, 18, Color.WHITE)

    if touch_moving:
        draw_circle(touch_move, 42, Color(1,1,1,0.18))
    draw_circle(Vector2(1140, 610), 55, Color(0,0,0,0.22))
    draw_string(ThemeDB.fallback_font, Vector2(1110, 617), "FIRE", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color.WHITE)

func draw_ellipse(center: Vector2, radii: Vector2, color: Color) -> void:
    var points := PackedVector2Array()
    for i in range(24):
        var a := TAU * float(i) / 24.0
        points.append(center + Vector2(cos(a) * radii.x, sin(a) * radii.y))
    draw_colored_polygon(points, color)
