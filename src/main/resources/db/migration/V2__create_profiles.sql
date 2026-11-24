CREATE TABLE profiles (
  id uuid PRIMARY KEY,
  display_name varchar(50),
  city_id uuid REFERENCES cities (id),
  radius_km integer NOT NULL DEFAULT 25 CHECK (radius_km BETWEEN 1 AND 100),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
