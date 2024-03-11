#!/usr/bin/env bash
#
# Copyright (C) 2024 NotEnoughUpdates contributors
#
# This file is part of NotEnoughUpdates.
#
# NotEnoughUpdates is free software: you can redistribute it
# and/or modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation, either
# version 3 of the License, or (at your option) any later version.
#
# NotEnoughUpdates is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
#

if [[ $# -ne 3 ]]; then
  echo "Usage: <keypath> <key name> <hash>"
  exit 1
fi

echo use key $1, label $2, signing hash $3
work=$(mktemp)
echo $work
echo "$3" | tr '[:lower:]' '[:upper:]' |tr -d '\n ' > "$work"
openssl dgst -sign "$1" "$work" > "$2.asc"
echo signature saved to "$2.asc"



