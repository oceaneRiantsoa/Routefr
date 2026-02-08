
--
-- PostgreSQL database dump
--

-- Dumped from database version 15.4 (Debian 15.4-1.pgdg110+1)
-- Dumped by pg_dump version 15.4 (Debian 15.4-1.pgdg110+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: tiger; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA tiger;


ALTER SCHEMA tiger OWNER TO postgres;

--
-- Name: tiger_data; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA tiger_data;


ALTER SCHEMA tiger_data OWNER TO postgres;

--
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- Name: SCHEMA topology; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA topology IS 'PostGIS Topology schema';


--
-- Name: fuzzystrmatch; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch WITH SCHEMA public;


--
-- Name: EXTENSION fuzzystrmatch; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION fuzzystrmatch IS 'determine similarities and distance between strings';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


--
-- Name: postgis_tiger_geocoder; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder WITH SCHEMA tiger;


--
-- Name: EXTENSION postgis_tiger_geocoder; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_tiger_geocoder IS 'PostGIS tiger geocoder and reverse geocoder';


--
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: entreprise; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.entreprise (
    id bigint NOT NULL,
    nom_entreprise character varying(150) NOT NULL,
    localisation character varying(200),
    contact character varying(100)
);


ALTER TABLE public.entreprise OWNER TO postgres;

--
-- Name: entreprise_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.entreprise_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.entreprise_id_seq OWNER TO postgres;

--
-- Name: entreprise_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.entreprise_id_seq OWNED BY public.entreprise.id;


--
-- Name: local_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.local_users (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    display_name character varying(255),
    role character varying(50) DEFAULT 'USER'::character varying,
    failed_attempts integer DEFAULT 0,
    account_locked boolean DEFAULT false,
    password_hash character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_login timestamp without time zone,
    firebase_sync_date timestamp without time zone,
    password_plain_temp character varying(255),
    synced_to_firebase boolean DEFAULT false
);


ALTER TABLE public.local_users OWNER TO postgres;

--
-- Name: local_users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.local_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.local_users_id_seq OWNER TO postgres;

--
-- Name: local_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.local_users_id_seq OWNED BY public.local_users.id;


--
-- Name: probleme; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.probleme (
    id bigint NOT NULL,
    nom character varying(100) NOT NULL,
    detail text,
    cout_par_m2 numeric(12,2) NOT NULL
);


ALTER TABLE public.probleme OWNER TO postgres;

--
-- Name: probleme_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.probleme_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.probleme_id_seq OWNER TO postgres;

--
-- Name: probleme_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.probleme_id_seq OWNED BY public.probleme.id;


--
-- Name: profils; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profils (
    id bigint NOT NULL,
    nom character varying(100) NOT NULL,
    id_role_value integer NOT NULL
);


ALTER TABLE public.profils OWNER TO postgres;

--
-- Name: profils_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profils_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profils_id_seq OWNER TO postgres;

--
-- Name: profils_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.profils_id_seq OWNED BY public.profils.id;


--
-- Name: security_settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_settings (
    id bigint NOT NULL,
    session_duration integer DEFAULT 30,
    max_login_attempts integer DEFAULT 5,
    lockout_duration integer DEFAULT 15,
    lockout_message text DEFAULT 'Votre compte a ete temporairement bloque.'::text,
    auto_lock_enabled boolean DEFAULT true,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.security_settings OWNER TO postgres;

--
-- Name: security_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.security_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.security_settings_id_seq OWNER TO postgres;

--
-- Name: security_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.security_settings_id_seq OWNED BY public.security_settings.id;


--
-- Name: signalement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.signalement (
    id bigint NOT NULL,
    idprofils integer NOT NULL,
    datetime_signalement timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.signalement OWNER TO postgres;

--
-- Name: signalement_details; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.signalement_details (
    id bigint NOT NULL,
    id_signalement integer NOT NULL,
    id_probleme integer NOT NULL,
    surface numeric(10,2) NOT NULL,
    id_entreprise integer,
    commentaires text,
    geom public.geography(Point,4326) NOT NULL,
    budget_estime numeric(15,2),
    entreprise_assignee character varying(200),
    notes_manager text,
    statut_manager character varying(50),
    date_modification timestamp without time zone
);


ALTER TABLE public.signalement_details OWNER TO postgres;

--
-- Name: signalement_details_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.signalement_details_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.signalement_details_id_seq OWNER TO postgres;

--
-- Name: signalement_details_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.signalement_details_id_seq OWNED BY public.signalement_details.id;


--
-- Name: signalement_firebase; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.signalement_firebase (
    id bigint NOT NULL,
    firebase_id character varying(255) NOT NULL,
    user_id character varying(255),
    user_email character varying(255),
    latitude double precision,
    longitude double precision,
    probleme_id character varying(100),
    probleme_nom character varying(200),
    description text,
    status character varying(50),
    surface numeric(10,2),
    budget numeric(15,2),
    date_creation_firebase timestamp without time zone,
    photo_url text,
    entreprise_id character varying(100),
    entreprise_nom character varying(200),
    notes_manager text,
    statut_local character varying(50) DEFAULT 'non_traite'::character varying,
    budget_estime numeric(15,2),
    date_synchronisation timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    date_modification_local timestamp without time zone,
    geom public.geography(Point,4326),
    avancement_pourcentage integer DEFAULT 0,
    date_debut_travaux timestamp without time zone,
    date_fin_travaux timestamp without time zone
);


ALTER TABLE public.signalement_firebase OWNER TO postgres;

--
-- Name: signalement_firebase_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.signalement_firebase_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.signalement_firebase_id_seq OWNER TO postgres;

--
-- Name: signalement_firebase_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.signalement_firebase_id_seq OWNED BY public.signalement_firebase.id;


--
-- Name: signalement_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.signalement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.signalement_id_seq OWNER TO postgres;

--
-- Name: signalement_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.signalement_id_seq OWNED BY public.signalement.id;


--
-- Name: signalement_status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.signalement_status (
    id bigint NOT NULL,
    id_signalement integer NOT NULL,
    idstatut integer NOT NULL
);


ALTER TABLE public.signalement_status OWNER TO postgres;

--
-- Name: signalement_status_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.signalement_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.signalement_status_id_seq OWNER TO postgres;

--
-- Name: signalement_status_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.signalement_status_id_seq OWNED BY public.signalement_status.id;


--
-- Name: user_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_sessions (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    session_token character varying(255) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    active boolean DEFAULT true,
    ip_address character varying(50),
    user_agent text
);


ALTER TABLE public.user_sessions OWNER TO postgres;

--
-- Name: user_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_sessions_id_seq OWNER TO postgres;

--
-- Name: user_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_sessions_id_seq OWNED BY public.user_sessions.id;


--
-- Name: vue_recapitulation; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.vue_recapitulation AS
 SELECT count(DISTINCT s.id) AS nb_point,
    sum(sd.surface) AS total_surface,
        CASE
            WHEN (min(st.idstatut) = 20) THEN 20
            WHEN (max(st.idstatut) >= 10) THEN 10
            ELSE 1
        END AS avancement,
    sum((sd.surface * p.cout_par_m2)) AS total_budget
   FROM (((public.signalement s
     JOIN public.signalement_details sd ON ((sd.id_signalement = s.id)))
     JOIN public.probleme p ON ((p.id = sd.id_probleme)))
     LEFT JOIN public.signalement_status st ON ((st.id_signalement = s.id)));


ALTER TABLE public.vue_recapitulation OWNER TO postgres;

--
-- Name: entreprise id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.entreprise ALTER COLUMN id SET DEFAULT nextval('public.entreprise_id_seq'::regclass);


--
-- Name: local_users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.local_users ALTER COLUMN id SET DEFAULT nextval('public.local_users_id_seq'::regclass);


--
-- Name: probleme id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.probleme ALTER COLUMN id SET DEFAULT nextval('public.probleme_id_seq'::regclass);


--
-- Name: profils id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profils ALTER COLUMN id SET DEFAULT nextval('public.profils_id_seq'::regclass);


--
-- Name: security_settings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_settings ALTER COLUMN id SET DEFAULT nextval('public.security_settings_id_seq'::regclass);


--
-- Name: signalement id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement ALTER COLUMN id SET DEFAULT nextval('public.signalement_id_seq'::regclass);


--
-- Name: signalement_details id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_details ALTER COLUMN id SET DEFAULT nextval('public.signalement_details_id_seq'::regclass);


--
-- Name: signalement_firebase id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_firebase ALTER COLUMN id SET DEFAULT nextval('public.signalement_firebase_id_seq'::regclass);


--
-- Name: signalement_status id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_status ALTER COLUMN id SET DEFAULT nextval('public.signalement_status_id_seq'::regclass);


--
-- Name: user_sessions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions ALTER COLUMN id SET DEFAULT nextval('public.user_sessions_id_seq'::regclass);


--
-- Data for Name: entreprise; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.entreprise (id, nom_entreprise, localisation, contact) FROM stdin;
1	Colas Madagascar	Ankorondrano	034 12 345 67
2	Ravinala Travaux	Ivato	034 98 765 43
\.


--
-- Data for Name: local_users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.local_users (id, firebase_uid, email, display_name, role, failed_attempts, account_locked, password_hash, created_at, last_login, firebase_sync_date, password_plain_temp, synced_to_firebase) FROM stdin;
1	uid_manager_default	manager@routefr.com	Manager Principal	MANAGER	0	f	\N	2026-02-07 10:04:48.157346	\N	\N	\N	f
2	uid_admin_001	admin@routefr.com	Admin Principal	ADMIN	0	f	\N	2026-02-07 10:04:48.157346	\N	\N	\N	f
3	uid_blocked_001	blocked@test.com	Utilisateur Bloque 1	USER	5	t	\N	2026-02-07 10:04:48.157346	\N	\N	\N	f
4	uid_blocked_002	blocked2@test.com	Utilisateur Bloque 2	USER	7	t	\N	2026-02-07 10:04:48.157346	\N	\N	\N	f
5	local-e1fe48b1-b3dd-4972-a158-b41be3270673	test@login.com	\N	USER	0	f	$2a$10$lcvHGkKuQArcitEZpbZppu3B5uoEp6R4ihKHpImRXpMDv2gocWgeu	2026-02-07 10:50:46.325338	2026-02-07 10:51:50.684038	\N	password123	f
\.


--
-- Data for Name: probleme; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.probleme (id, nom, detail, cout_par_m2) FROM stdin;
1	Nid de poule	Trou sur la chaussee	50000.00
2	Route fissuree	Fissures importantes	30000.00
3	Affaissement	Affaissement de la chaussee	75000.00
\.


--
-- Data for Name: profils; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.profils (id, nom, id_role_value) FROM stdin;
1	Ocy	10
2	Rado	20
\.


--
-- Data for Name: security_settings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.security_settings (id, session_duration, max_login_attempts, lockout_duration, lockout_message, auto_lock_enabled, updated_at) FROM stdin;
1	60	5	15	Votre compte a ete temporairement bloque.	t	2026-02-07 10:04:48.159211
\.


--
-- Data for Name: signalement; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.signalement (id, idprofils, datetime_signalement) FROM stdin;
1	1	2026-01-10 08:30:00
2	2	2026-01-12 14:15:00
\.


--
-- Data for Name: signalement_details; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.signalement_details (id, id_signalement, id_probleme, surface, id_entreprise, commentaires, geom, budget_estime, entreprise_assignee, notes_manager, statut_manager, date_modification) FROM stdin;
1	1	1	12.55	1	Pres arret bus	0101000020E6100000C442AD69DEC14740A9A44E4013E132C0	\N	\N	\N	\N	2026-02-07 10:54:01.523004
2	2	2	200.00	1	Route fissuree	0101000020E61000008716D9CEF7C347408FC2F5285CDF32C0	\N	\N	\N	\N	2026-02-07 10:56:57.99406
\.


--
-- Data for Name: signalement_firebase; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.signalement_firebase (id, firebase_id, user_id, user_email, latitude, longitude, probleme_id, probleme_nom, description, status, surface, budget, date_creation_firebase, photo_url, entreprise_id, entreprise_nom, notes_manager, statut_local, budget_estime, date_synchronisation, date_modification_local, geom, avancement_pourcentage, date_debut_travaux, date_fin_travaux) FROM stdin;
1	-OjQgprdrR1SCSPqmkcf	kHoDlj8SoMPvEYdNPHfNcNb6Ken1	ioty.ratisbonne@gmail.com	-18.888790440129274	47.52535560249385	affaissement	Affaissement	nn	nouveau	\N	\N	2026-01-20 13:58:43.414	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:18.739569	\N	0101000020E6100000A1BD35DA3EC34740495A31C587E332C0	0	\N	\N
2	-Ojk45CPKtNOLBGTeIfF	kHoDlj8SoMPvEYdNPHfNcNb6Ken1	ioty.ratisbonne@gmail.com	-18.8700944957777	47.50368118286133	inondation	Inondation		nouveau	\N	\N	2026-01-24 12:57:06.572	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.154412	\N	0101000020E6100000000000A078C047407ACB4B83BEDE32C0	0	\N	\N
3	-OjphEWw9yrogJ6rXgSV	kHoDlj8SoMPvEYdNPHfNcNb6Ken1	ioty.ratisbonne@gmail.com	-18.8656328355113	47.486265601256655	fissure	Fissure		nouveau	\N	\N	2026-01-25 15:10:36.281	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.159813	\N	0101000020E6100000974883F33DBE474062DD0E1D9ADD32C0	0	\N	\N
4	-OkXMdbOVJW65XwA4Haj	kHoDlj8SoMPvEYdNPHfNcNb6Ken1	ioty.ratisbonne@gmail.com	-18.88870744107782	47.49973297119141	nid_poule	Nid de poule		nouveau	\N	\N	2026-02-03 07:19:10.853	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.164675	\N	0101000020E610000001000040F7BF47401E24B35482E332C0	0	\N	\N
5	-OkXMfUZXXQHxMtboClA	kHoDlj8SoMPvEYdNPHfNcNb6Ken1	ioty.ratisbonne@gmail.com	-18.986031019607843	47.5325061862745	eclairage	Éclairage défaillant		nouveau	\N	\N	2026-02-03 07:19:18.544	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.171148	\N	0101000020E6100000BC7BA72929C44740A60E66876CFC32C0	0	\N	\N
6	test1	test_user_id	test@test.com	-18.8792	47.5079	nid_poule	Nid de poule	Nid de poule important devant la mairie d'Analakely	nouveau	15.00	300000.00	2025-01-20 00:00:00	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.17743	\N	0101000020E6100000B7D100DE02C14740A9A44E4013E132C0	0	\N	\N
7	test2	test_user_id	test@test.com	-18.885	47.515	affaissement	Affaissement	Affaissement de chaussée près du Lac Anosy	en_cours	50.00	1500000.00	2025-01-19 00:00:00	\N	\N	\N	\N	non_traite	\N	2026-02-07 10:53:19.183404	\N	0101000020E610000052B81E85EBC14740C3F5285C8FE232C0	0	\N	\N
\.


--
-- Data for Name: signalement_status; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.signalement_status (id, id_signalement, idstatut) FROM stdin;
2	2	20
1	1	30
\.


--
-- Data for Name: spatial_ref_sys; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.spatial_ref_sys (srid, auth_name, auth_srid, srtext, proj4text) FROM stdin;
\.


--
-- Data for Name: user_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_sessions (id, firebase_uid, session_token, created_at, expires_at, active, ip_address, user_agent) FROM stdin;
1	local-e1fe48b1-b3dd-4972-a158-b41be3270673	27d405f1-ecfc-4bf6-a310-5bd0bb7eed4c	2026-02-07 10:50:46.427467	2026-02-07 11:50:46.427467	t	172.19.0.1	curl/8.5.0
2	local-e1fe48b1-b3dd-4972-a158-b41be3270673	02d1dddf-38ae-4313-be50-4f28129cdc72	2026-02-07 10:51:03.506328	2026-02-07 11:51:03.506328	t	172.19.0.1	curl/8.5.0
3	local-e1fe48b1-b3dd-4972-a158-b41be3270673	2380b840-3058-4136-97e3-b09dc94361b1	2026-02-07 10:51:50.69872	2026-02-07 11:51:50.69872	t	172.19.0.1	Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:147.0) Gecko/20100101 Firefox/147.0
\.


--
-- Data for Name: geocode_settings; Type: TABLE DATA; Schema: tiger; Owner: postgres
--

COPY tiger.geocode_settings (name, setting, unit, category, short_desc) FROM stdin;
\.


--
-- Data for Name: pagc_gaz; Type: TABLE DATA; Schema: tiger; Owner: postgres
--

COPY tiger.pagc_gaz (id, seq, word, stdword, token, is_custom) FROM stdin;
\.


--
-- Data for Name: pagc_lex; Type: TABLE DATA; Schema: tiger; Owner: postgres
--

COPY tiger.pagc_lex (id, seq, word, stdword, token, is_custom) FROM stdin;
\.


--
-- Data for Name: pagc_rules; Type: TABLE DATA; Schema: tiger; Owner: postgres
--

COPY tiger.pagc_rules (id, rule, is_custom) FROM stdin;
\.


--
-- Data for Name: topology; Type: TABLE DATA; Schema: topology; Owner: postgres
--

COPY topology.topology (id, name, srid, "precision", hasz) FROM stdin;
\.


--
-- Data for Name: layer; Type: TABLE DATA; Schema: topology; Owner: postgres
--

COPY topology.layer (topology_id, layer_id, schema_name, table_name, feature_column, feature_type, level, child_id) FROM stdin;
\.


--
-- Name: entreprise_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.entreprise_id_seq', 2, true);


--
-- Name: local_users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.local_users_id_seq', 5, true);


--
-- Name: probleme_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.probleme_id_seq', 3, true);


--
-- Name: profils_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.profils_id_seq', 2, true);


--
-- Name: security_settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.security_settings_id_seq', 1, true);


--
-- Name: signalement_details_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.signalement_details_id_seq', 2, true);


--
-- Name: signalement_firebase_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.signalement_firebase_id_seq', 7, true);


--
-- Name: signalement_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.signalement_id_seq', 2, true);


--
-- Name: signalement_status_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.signalement_status_id_seq', 2, true);


--
-- Name: user_sessions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.user_sessions_id_seq', 3, true);


--
-- Name: topology_id_seq; Type: SEQUENCE SET; Schema: topology; Owner: postgres
--

SELECT pg_catalog.setval('topology.topology_id_seq', 1, false);


--
-- Name: entreprise entreprise_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.entreprise
    ADD CONSTRAINT entreprise_pkey PRIMARY KEY (id);


--
-- Name: local_users local_users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.local_users
    ADD CONSTRAINT local_users_email_key UNIQUE (email);


--
-- Name: local_users local_users_firebase_uid_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.local_users
    ADD CONSTRAINT local_users_firebase_uid_key UNIQUE (firebase_uid);


--
-- Name: local_users local_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.local_users
    ADD CONSTRAINT local_users_pkey PRIMARY KEY (id);


--
-- Name: probleme probleme_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.probleme
    ADD CONSTRAINT probleme_pkey PRIMARY KEY (id);


--
-- Name: profils profils_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profils
    ADD CONSTRAINT profils_pkey PRIMARY KEY (id);


--
-- Name: security_settings security_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_settings
    ADD CONSTRAINT security_settings_pkey PRIMARY KEY (id);


--
-- Name: signalement_details signalement_details_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_details
    ADD CONSTRAINT signalement_details_pkey PRIMARY KEY (id);


--
-- Name: signalement_firebase signalement_firebase_firebase_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_firebase
    ADD CONSTRAINT signalement_firebase_firebase_id_key UNIQUE (firebase_id);


--
-- Name: signalement_firebase signalement_firebase_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_firebase
    ADD CONSTRAINT signalement_firebase_pkey PRIMARY KEY (id);


--
-- Name: signalement signalement_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement
    ADD CONSTRAINT signalement_pkey PRIMARY KEY (id);


--
-- Name: signalement_status signalement_status_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_status
    ADD CONSTRAINT signalement_status_pkey PRIMARY KEY (id);


--
-- Name: user_sessions user_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pkey PRIMARY KEY (id);


--
-- Name: user_sessions user_sessions_session_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_session_token_key UNIQUE (session_token);


--
-- Name: idx_local_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_local_users_email ON public.local_users USING btree (email);


--
-- Name: idx_local_users_firebase_uid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_local_users_firebase_uid ON public.local_users USING btree (firebase_uid);


--
-- Name: idx_sessions_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sessions_active ON public.user_sessions USING btree (active);


--
-- Name: idx_sessions_firebase_uid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sessions_firebase_uid ON public.user_sessions USING btree (firebase_uid);


--
-- Name: idx_sessions_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sessions_token ON public.user_sessions USING btree (session_token);


--
-- Name: idx_signalement_firebase_geom; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signalement_firebase_geom ON public.signalement_firebase USING gist (geom);


--
-- Name: idx_signalement_firebase_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signalement_firebase_id ON public.signalement_firebase USING btree (firebase_id);


--
-- Name: idx_signalement_geom; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signalement_geom ON public.signalement_details USING gist (geom);


--
-- Name: signalement_details fk_details_entreprise; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_details
    ADD CONSTRAINT fk_details_entreprise FOREIGN KEY (id_entreprise) REFERENCES public.entreprise(id);


--
-- Name: signalement_details fk_details_probleme; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_details
    ADD CONSTRAINT fk_details_probleme FOREIGN KEY (id_probleme) REFERENCES public.probleme(id);


--
-- Name: signalement_details fk_details_signalement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_details
    ADD CONSTRAINT fk_details_signalement FOREIGN KEY (id_signalement) REFERENCES public.signalement(id);


--
-- Name: user_sessions fk_session_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT fk_session_user FOREIGN KEY (firebase_uid) REFERENCES public.local_users(firebase_uid) ON DELETE CASCADE;


--
-- Name: signalement fk_signalement_profils; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement
    ADD CONSTRAINT fk_signalement_profils FOREIGN KEY (idprofils) REFERENCES public.profils(id);


--
-- Name: signalement_status fk_status_signalement; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.signalement_status
    ADD CONSTRAINT fk_status_signalement FOREIGN KEY (id_signalement) REFERENCES public.signalement(id);


--
-- PostgreSQL database dump complete
--

